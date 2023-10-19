export const IS_TOUCH_DEVICE = (('ontouchstart' in window)
    || (navigator.maxTouchPoints > 0)
    || (navigator.msMaxTouchPoints > 0));

export const DEV_MODE = window.LAUNCH_MODE === 'DEVELOPMENT';

console.log(`dev-mode: ${DEV_MODE}`);