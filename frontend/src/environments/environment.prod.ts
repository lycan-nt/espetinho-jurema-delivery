const origin = typeof globalThis !== 'undefined' && 'location' in globalThis
  ? (globalThis as unknown as { location: { origin: string } }).location.origin
  : '';

export const environment = {
  production: true,
  apiUrl: '',
  wsUrl: `${origin}/ws`,
};
