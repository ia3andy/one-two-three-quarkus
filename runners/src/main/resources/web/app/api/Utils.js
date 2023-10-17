export const IS_TOUCH_DEVICE = (('ontouchstart' in window)
    || (navigator.maxTouchPoints > 0)
    || (navigator.msMaxTouchPoints > 0));

export const DEV_MODE = !process.env.NODE_ENV || process.env.NODE_ENV === 'development';

console.log(`dev-mode: ${DEV_MODE}`);