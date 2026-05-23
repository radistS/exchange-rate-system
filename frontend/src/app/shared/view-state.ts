/**
 * Async UI state for feature screens: idle → loading → data | error.
 * Templates must handle all four branches explicitly.
 */
export type ViewState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'data'; data: T }
  | { status: 'error'; error: string };

export function idle<T>(): ViewState<T> {
  return { status: 'idle' };
}

export function loading<T>(): ViewState<T> {
  return { status: 'loading' };
}

export function data<T>(value: T): ViewState<T> {
  return { status: 'data', data: value };
}

export function error<T>(message: string): ViewState<T> {
  return { status: 'error', error: message };
}
