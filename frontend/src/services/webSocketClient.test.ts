import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  calculateReconnectDelay,
  createInitialWebSocketState,
  shouldReconnectAfterClose,
  transitionWebSocketState,
} from './webSocketClient.js';

test('calculateReconnectDelay applies exponential backoff, jitter, and the configured cap', () => {
  assert.equal(
    calculateReconnectDelay(2, {
      baseDelayMs: 1000,
      jitterMs: 1000,
      maxDelayMs: 30000,
      random: () => 0.5,
    }),
    4500
  );

  assert.equal(
    calculateReconnectDelay(10, {
      baseDelayMs: 1000,
      jitterMs: 1000,
      maxDelayMs: 30000,
      random: () => 1,
    }),
    30000
  );
});

test('transitionWebSocketState models reconnect state and resets after a stable connection', () => {
  const initial = createInitialWebSocketState();
  const reconnecting = transitionWebSocketState(initial, {
    type: 'unexpected-close',
    nextAttempt: 1,
  });

  assert.equal(reconnecting.connectionState, 'reconnecting');
  assert.equal(reconnecting.reconnectAttempt, 1);

  const connected = transitionWebSocketState(reconnecting, { type: 'stable-open' });

  assert.equal(connected.connectionState, 'connected');
  assert.equal(connected.reconnectAttempt, 0);
  assert.equal(connected.errors, 0);
});

test('transitionWebSocketState clears reconnecting state when reconnect attempts are exhausted', () => {
  const initial = createInitialWebSocketState();
  const exhausted = transitionWebSocketState(initial, { type: 'reconnect-exhausted' });

  assert.equal(exhausted.connectionState, 'error');
  assert.equal(exhausted.reconnectAttempt, 0);
});

test('shouldReconnectAfterClose only reconnects after unexpected closes', () => {
  assert.equal(shouldReconnectAfterClose({ code: 1000 }), false);
  assert.equal(shouldReconnectAfterClose({ code: 1001 }), false);
  assert.equal(shouldReconnectAfterClose({ code: 1006 }), true);
  assert.equal(shouldReconnectAfterClose({ code: 4000 }), true);
});
