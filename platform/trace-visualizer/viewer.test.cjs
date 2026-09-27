"use strict";
const {test} = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {parseTrace, stateAt, formatTime} = require("./viewer.js");
const traceDirectory = path.resolve(__dirname, "../simulator/build/traces");
const read = name => parseTrace(fs.readFileSync(path.join(traceDirectory, name + ".json"), "utf8"));
const end = trace => stateAt(trace, trace.events.length - 1);

test("all eight Java exports are valid viewer inputs", () => {
    for (const name of ["scheduler-order", "success", "lost-request", "lost-reply", "late-reply",
        "duplicate-reply", "reply-first", "timeout-first"]) {
        const trace = read(name);
        assert.ok(trace.events.length > 0, name);
        assert.equal(end(trace).timers.size, 0, name);
    }
});

test("lost request and lost reply have identical client outcomes and different server states", () => {
    const request = end(read("lost-request")), reply = end(read("lost-reply"));
    assert.equal(request.states.get("client"), "TIMED_OUT");
    assert.equal(reply.states.get("client"), "TIMED_OUT");
    assert.equal(request.states.get("server"), "");
    assert.equal(reply.states.get("server"), "x");
    assert.equal([...request.messages.values()].find(m => m.status === "Dropped").source, "client");
    assert.equal([...reply.messages.values()].find(m => m.status === "Dropped").source, "server");
});

test("rewinding does not leak a later server mutation or completion", () => {
    const trace = read("lost-reply");
    const index = trace.events.findIndex(e => e.kind === "STATE_CHANGED"
        && e.attributes.node === "server" && e.attributes.value === "x");
    assert.equal(end(trace).states.get("server"), "x");
    assert.equal(stateAt(trace, index - 1).states.get("server"), "");
    assert.equal(stateAt(trace, index - 1).states.get("client"), "PENDING");
    assert.equal(stateAt(trace, -1).states.size, 0);
    assert.equal(stateAt(trace, -1).messages.size, 0);
});

test("equal-time races and duplicate replies retain the Java scenario outcomes", () => {
    assert.equal(end(read("reply-first")).states.get("client"), "SUCCEEDED");
    assert.equal(end(read("timeout-first")).states.get("client"), "TIMED_OUT");
    assert.equal(end(read("duplicate-reply")).states.get("client"), "SUCCEEDED");
    assert.equal(end(read("late-reply")).states.get("client"), "TIMED_OUT");
});

test("scheduler-only histories expose timers and cancellation", () => {
    const trace = read("scheduler-order");
    const cancelled = trace.events.findIndex(e => e.kind === "TASK_CANCELLED");
    assert.equal(stateAt(trace, cancelled - 1).timers.size, 3);
    assert.equal(stateAt(trace, cancelled).timers.size, 2);
    assert.equal(end(trace).timers.size, 0);
});

test("empty traces and nanoseconds beyond safe integer precision are supported", () => {
    const empty = parseTrace('{"schemaVersion":1,"events":[]}');
    assert.equal(stateAt(empty, -1).states.size, 0);
    assert.equal(formatTime("9007200000000001"), "9007200.000000001s");
    assert.equal(formatTime("0"), "0s");
    assert.equal(formatTime("5000000"), "0.005s");
});

test("malformed or incompatible histories fail explicitly", () => {
    assert.throws(() => parseTrace("{"), SyntaxError);
    assert.throws(() => parseTrace('{"schemaVersion":2,"events":[]}'), /schemaVersion/);
    for (const mutate of [
        trace => {trace.events[0].sequence = 5;},
        trace => {trace.events[0].logicalTimeNanos = "NaN";},
        trace => {trace.events[1].attributes = null;},
        trace => {trace.events[0].logicalTimeNanos = "999999999";},
        trace => {delete trace.events.find(e => e.kind === "MESSAGE_SENT").attributes.source;},
        trace => {trace.events.find(e => e.kind === "TASK_SCHEDULED").attributes.dueNanos = "invalid";},
        trace => {delete trace.events.find(e => e.kind === "TASK_STARTED").attributes.taskId;}
    ]) {
        const trace = read("success");
        mutate(trace);
        assert.throws(() => parseTrace(JSON.stringify(trace)));
    }
    assert.throws(() => stateAt(read("success"), -2), /Cursor/);
});

test("application strings remain data, including prototype-like keys and markup", () => {
    const trace = read("success");
    trace.events[0].attributes.node = "__proto__";
    trace.events[0].attributes.value = "<img src=x onerror=alert(1)>";
    const parsed = parseTrace(JSON.stringify(trace));
    assert.equal(stateAt(parsed, 0).states.get("__proto__"), "<img src=x onerror=alert(1)>");
    assert.equal({}.polluted, undefined);
});
