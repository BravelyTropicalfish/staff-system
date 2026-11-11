/** The two addresses the harness knows: the pages and the API the fixtures speak to directly. */
export const APP = process.env.E2E_BASE_URL ?? 'http://localhost:5173';

export const API = process.env.E2E_API_URL ?? 'http://localhost:8080';

/** The one password the seed gives every account. */
export const PASSWORD = 'staffing-demo';

/** Where `auth.setup.ts` leaves the signed-in storage states. */
export const COORDINATOR = 'e2e/.auth/coordinator.json';

export const LEAD_ONE = 'e2e/.auth/lead-one.json';

export const LEAD_TWO = 'e2e/.auth/lead-two.json';
