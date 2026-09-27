/* Works offline as a classic browser script; its pure model also runs in Node tests. */
"use strict";
const TraceViewer = (() => {
    function parseTrace(text) {
        const trace = JSON.parse(text);
        if (!trace || trace.schemaVersion !== 1 || !Array.isArray(trace.events)) {
            throw new Error("Expected a schemaVersion 1 trace with an events array.");
        }
        let previousTime = -1n;
        for (const [index, event] of trace.events.entries()) {
            if (!event || event.schemaVersion !== 1 || event.sequence !== index
                    || typeof event.logicalTimeNanos !== "string" || !/^\d+$/.test(event.logicalTimeNanos)
                    || typeof event.kind !== "string" || typeof event.subject !== "string"
                    || !event.attributes || typeof event.attributes !== "object" || Array.isArray(event.attributes)
                    || Object.values(event.attributes).some(value => typeof value !== "string")) {
                throw new Error("Invalid trace event at index " + index + ".");
            }
            const time = BigInt(event.logicalTimeNanos);
            if (time < previousTime) throw new Error("Event times must not move backward.");
            previousTime = time;
            if (event.kind.startsWith("MESSAGE_")
                    && ["messageId", "requestId", "source", "destination", "payload"]
                        .some(key => typeof event.attributes[key] !== "string")) {
                throw new Error("Message event is missing its identity or endpoints.");
            }
            if (event.kind === "STATE_CHANGED"
                    && ["node", "value"].some(key => typeof event.attributes[key] !== "string")) {
                throw new Error("State observation is missing node or value.");
            }
            if (event.kind.startsWith("TASK_") && (typeof event.attributes.taskId !== "string"
                    || !/^\d+$/.test(event.attributes.taskId))) {
                throw new Error("Task event is missing its identity.");
            }
            if (event.kind === "TASK_SCHEDULED" && (typeof event.attributes.dueNanos !== "string"
                    || !/^\d+$/.test(event.attributes.dueNanos))) {
                throw new Error("Scheduled event must have a non-negative due time.");
            }
        }
        return trace;
    }

    function formatTime(nanos) {
        const time = BigInt(nanos);
        const fraction = (time % 1000000000n).toString().padStart(9, "0").replace(/0+$/, "");
        return (time / 1000000000n).toString() + (fraction ? "." + fraction : "") + "s";
    }

    // Reconstruct only the selected prefix, so stepping backward never leaks future state.
    function stateAt(trace, cursor) {
        if (!Number.isInteger(cursor) || cursor < -1 || cursor >= trace.events.length) {
            throw new Error("Cursor is outside the trace.");
        }
        const states = new Map(), timers = new Map(), messages = new Map();
        for (const event of trace.events.slice(0, cursor + 1)) {
            const attr = event.attributes;
            if (event.kind === "STATE_CHANGED") states.set(attr.node, attr.value);
            if (event.kind === "TASK_SCHEDULED") timers.set(attr.taskId, {
                label: event.subject, due: attr.dueNanos
            });
            if (event.kind === "TASK_CANCELLED" || event.kind === "TASK_STARTED") timers.delete(attr.taskId);
            if (event.kind === "MESSAGE_SENT") messages.set(attr.messageId, {
                ...attr, status: "In flight", sentAt: event.logicalTimeNanos, observedAt: event.logicalTimeNanos
            });
            if (event.kind === "MESSAGE_DELIVERED" || event.kind === "MESSAGE_DROPPED") {
                const message = messages.get(attr.messageId);
                if (message) {
                    message.status = event.kind === "MESSAGE_DELIVERED" ? "Delivered" : "Dropped";
                    message.observedAt = event.logicalTimeNanos;
                }
            }
        }
        return {states, timers, messages};
    }

    function mount() {
        const get = id => document.getElementById(id);
        let trace = {schemaVersion: 1, events: []}, cursor = -1, loaded = false;
        function element(tag, text, parent, className) {
            const node = document.createElement(tag);
            node.textContent = text;
            if (className) node.className = className;
            parent.append(node);
            return node;
        }
        function svgElement(tag, attributes, parent, text) {
            const node = document.createElementNS("http://www.w3.org/2000/svg", tag);
            for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, String(value));
            if (text !== undefined) node.textContent = text;
            parent.append(node);
            return node;
        }
        function drawMessages(messages) {
            const container = get("diagram");
            container.replaceChildren();
            if (!messages.size) {
                element("p", "No messages sent at this point.", container, "empty");
                return;
            }
            const nodes = [...new Set([...messages.values()].flatMap(m => [m.source, m.destination]))];
            const width = Math.max(540, nodes.length * 220);
            const height = 90 + messages.size * 75;
            const svg = svgElement("svg", {viewBox: "0 0 " + width + " " + height,
                role: "img", "aria-label": "Messages through the selected event"}, container);
            const x = name => 100 + nodes.indexOf(name) * (width - 200) / Math.max(1, nodes.length - 1);
            for (const name of nodes) {
                svgElement("line", {x1:x(name), x2:x(name), y1:35, y2:height - 12,
                    stroke:"#d9e2e5", "stroke-dasharray":"4 5"}, svg);
                svgElement("text", {x:x(name), y:20, "text-anchor":"middle", fill:"#172e3c",
                    "font-size":14, "font-weight":650}, svg, name);
            }
            let row = 0;
            for (const message of messages.values()) {
                const y = 65 + row++ * 75, from = x(message.source), to = x(message.destination);
                const color = message.status === "Dropped" ? "#b73b4a"
                    : message.status === "Delivered" ? "#087d79" : "#7b641d";
                const end = message.status === "Dropped" ? (from + to) / 2 : to;
                svgElement("line", {x1:from, y1:y, x2:end, y2:y, stroke:color, "stroke-width":2,
                    "stroke-dasharray":message.status === "In flight" ? "5 4" : "none"}, svg);
                if (message.status === "Dropped") {
                    svgElement("text", {x:end, y:y + 5, fill:color, "text-anchor":"middle",
                        "font-size":22}, svg, "×");
                } else {
                    const direction = to >= from ? 1 : -1;
                    svgElement("path", {d:"M " + (to - 9 * direction) + " " + (y - 5)
                        + " L " + to + " " + y + " L " + (to - 9 * direction) + " " + (y + 5),
                        fill:"none", stroke:color, "stroke-width":2}, svg);
                }
                svgElement("text", {x:width / 2, y:y - 12, "text-anchor":"middle",
                    fill:color, "font-size":12}, svg,
                    "#" + message.messageId + " · " + message.requestId + " · " + message.status);
                svgElement("text", {x:width / 2, y:y + 22, "text-anchor":"middle",
                    fill:"#586d78", "font-size":11}, svg,
                    formatTime(message.sentAt) + " → " + formatTime(message.observedAt)
                    + " · payload: " + message.payload);
            }
        }
        function render() {
            const model = stateAt(trace, cursor), event = trace.events[cursor];
            get("start").disabled = get("previous").disabled = cursor < 0;
            get("next").disabled = get("end").disabled = cursor >= trace.events.length - 1;
            get("cursor").disabled = !trace.events.length;
            get("cursor").max = String(trace.events.length - 1);
            get("cursor").value = String(cursor);
            get("position").textContent = !loaded ? "Open a trace to begin"
                : !trace.events.length ? "Empty trace · 0 events"
                : cursor < 0 ? "Before first event · " + trace.events.length + " events"
                : "Event " + (cursor + 1) + " / " + trace.events.length + " · " + formatTime(event.logicalTimeNanos);
            get("states").replaceChildren();
            if (!model.states.size) element("p", "No state observations yet.", get("states"));
            for (const [node, value] of model.states) {
                const card = element("div", "", get("states"), "state");
                element("span", node, card);
                element("strong", value === "" ? '"" (empty)' : value, card);
            }
            get("timers").replaceChildren();
            if (!model.timers.size) element("li", "No pending events.", get("timers"));
            for (const [id, timer] of model.timers) {
                element("li", "#" + id + " " + timer.label
                    + (timer.due ? " · due " + formatTime(timer.due) : ""), get("timers"));
            }
            drawMessages(model.messages);
            get("details").textContent = event ? JSON.stringify(event, null, 2) : "No event selected.";
            for (const button of get("events").querySelectorAll("button")) {
                button.setAttribute("aria-current", String(Number(button.dataset.index) === cursor));
            }
        }
        function load(text, name) {
            const parsed = parseTrace(text); // A failed import leaves the previous history intact.
            trace = parsed; cursor = -1; loaded = true;
            get("filename").textContent = name;
            get("error").textContent = "";
            get("events").replaceChildren();
            if (!trace.events.length) element("p", "This trace contains no events.", get("events"));
            for (const [index, event] of trace.events.entries()) {
                const button = element("button", "#" + event.sequence + " · " + formatTime(event.logicalTimeNanos)
                    + " · " + event.kind + " · " + event.subject, get("events"));
                button.dataset.index = String(index);
                button.addEventListener("click", () => {cursor = index; render();});
            }
            render();
        }
        const report = error => {get("error").textContent = "Could not open trace: " + error.message;};
        get("trace-file").addEventListener("change", async event => {
            const file = event.target.files[0];
            if (file) {
                try {load(await file.text(), file.name);} catch (error) {report(error);}
            }
        });
        get("load-paste").addEventListener("click", () => {
            try {load(get("trace-paste").value, "Pasted trace");} catch (error) {report(error);}
        });
        get("start").addEventListener("click", () => {cursor = -1; render();});
        get("previous").addEventListener("click", () => {cursor--; render();});
        get("next").addEventListener("click", () => {cursor++; render();});
        get("end").addEventListener("click", () => {cursor = trace.events.length - 1; render();});
        get("cursor").addEventListener("input", event => {cursor = Number(event.target.value); render();});
        render();
    }
    return {parseTrace, formatTime, stateAt, mount};
})();
if (typeof module !== "undefined") module.exports = TraceViewer;
if (typeof document !== "undefined") TraceViewer.mount();
