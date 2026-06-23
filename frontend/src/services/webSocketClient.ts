export type WebSocketConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'error';

export interface WebSocketClientState {
  connectionState: WebSocketConnectionState;
  lastMessage: unknown | null;
  reconnectAttempt: number;
  queueSize: number;
  subscriptions: number;
  totalMessagesSent: number;
  totalMessagesReceived: number;
  errors: number;
  latencyMs: number | null;
}

export interface ReconnectDelayOptions {
  baseDelayMs?: number;
  maxDelayMs?: number;
  jitterMs?: number;
  random?: () => number;
}

export type WebSocketStateTransition =
  | { type: 'connect' }
  | { type: 'stable-open' }
  | { type: 'unexpected-close'; nextAttempt: number }
  | { type: 'disconnect' }
  | { type: 'error' }
  | { type: 'reconnect-exhausted' };

const DEFAULT_BASE_DELAY_MS = 1000;
const DEFAULT_MAX_DELAY_MS = 30000;
const DEFAULT_JITTER_MS = 1000;

export function calculateReconnectDelay(attempt: number, options: ReconnectDelayOptions = {}): number {
  const baseDelayMs = Math.max(0, options.baseDelayMs ?? DEFAULT_BASE_DELAY_MS);
  const maxDelayMs = Math.max(0, options.maxDelayMs ?? DEFAULT_MAX_DELAY_MS);
  const jitterMs = Math.max(0, options.jitterMs ?? DEFAULT_JITTER_MS);
  const random = options.random ?? Math.random;
  const safeAttempt = Math.max(0, attempt);
  const jitter = random() * jitterMs;
  const exponentialDelay = baseDelayMs * Math.pow(2, safeAttempt);

  return Math.min(Math.round(exponentialDelay + jitter), maxDelayMs);
}

export function createInitialWebSocketState(): WebSocketClientState {
  return {
    connectionState: 'disconnected',
    lastMessage: null,
    reconnectAttempt: 0,
    queueSize: 0,
    subscriptions: 0,
    totalMessagesSent: 0,
    totalMessagesReceived: 0,
    errors: 0,
    latencyMs: null,
  };
}

export function transitionWebSocketState(
  state: WebSocketClientState,
  transition: WebSocketStateTransition
): WebSocketClientState {
  switch (transition.type) {
    case 'connect':
      return { ...state, connectionState: 'connecting' };
    case 'stable-open':
      return { ...state, connectionState: 'connected', reconnectAttempt: 0, errors: 0 };
    case 'unexpected-close':
      return {
        ...state,
        connectionState: 'reconnecting',
        reconnectAttempt: Math.max(0, transition.nextAttempt),
      };
    case 'disconnect':
      return { ...state, connectionState: 'disconnected', reconnectAttempt: 0, latencyMs: null };
    case 'error':
      return { ...state, connectionState: 'error', errors: state.errors + 1 };
    case 'reconnect-exhausted':
      return { ...state, connectionState: 'error' };
  }
}

export function shouldReconnectAfterClose(event: Pick<CloseEvent, 'code'>): boolean {
  return event.code !== 1000 && event.code !== 1001;
}
