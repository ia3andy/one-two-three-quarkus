import { DEV_MODE } from './api/Utils';


export const API_CONFIG = window.API_CONFIG;

// Dashboard
export const TAP_DISTANCE = DEV_MODE ? 10 : 1;

// Mobile app
export const ENABLE_TAPPING = true;
export const ENABLE_SHAKING = true; // 'false' in v1, set to 'true' in v2
export const ENABLE_SWIPING = false;

// LOGGING
console.log(`Tap distance: ${TAP_DISTANCE}`);
console.log('Swiping Sensor: ', ENABLE_SWIPING);
console.log('Tapping Sensor: ', ENABLE_TAPPING);
console.log('Shaking Sensor: ', ENABLE_SHAKING);
