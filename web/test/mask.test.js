// Mirrors app/src/test/.../family/MaskTest.kt so the phone and the server mask the same way.
import assert from "node:assert/strict";
import { test } from "node:test";
import { maskNumbers } from "../lib/mask.js";

test("masks phone numbers", () => {
  assert.equal(maskNumbers("Call +91 98765 43210 now"), "Call +•• ••••• •••10 now");
  assert.equal(maskNumbers("Call 9876543210"), "Call ••••••••10");
});

test("masks account and card numbers", () => {
  assert.equal(maskNumbers("A/c 123456789012389 blocked"), "A/c •••••••••••••89 blocked");
  assert.equal(maskNumbers("4111 1111 1111 1111"), "•••• •••• •••• ••11");
  assert.equal(maskNumbers("OTP 123456"), "OTP ••••56");
});

test("keeps amounts and short numbers", () => {
  const text = "Pay Rs 1,00,000 or 5000 in 24 hours";
  assert.equal(maskNumbers(text), text);
});

test("keeps length", () => {
  const text = "Send 9876543210 to KYC 4111-1111-1111";
  assert.equal(maskNumbers(text).length, text.length);
});

test("is idempotent, so masking on the phone and the server is safe", () => {
  const once = maskNumbers("Call 9876543210");
  assert.equal(maskNumbers(once), once);
});
