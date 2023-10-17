import Shake from './Shake';

export function startSwipeSensor(handler) {
  let startX = 0;
  let startY = 0;

  document.addEventListener('touchstart', (e) => {
    startX = e.changedTouches[0].screenX;
    startY = e.changedTouches[0].screenY;
  });

  document.addEventListener('touchend', (e) => {
    const xDiff = startX - e.changedTouches[0].screenX;
    const yDiff = startY - e.changedTouches[0].screenY;
    if (Math.abs(xDiff) > Math.abs(yDiff)) { /* most significant */
      if (xDiff > 0) {
        /* right swipe */
        handler('right', xDiff);
      } else {
        /* left swipe */
        handler('left', xDiff);
      }
    } else if (yDiff > 0) {
      /* down swipe */
      handler('down', yDiff);
    } else {
      /* up swipe */
      handler('up', yDiff);
    }
  });
}

let shake;

export function enableShakeSensor() {
  shake = new Shake();
}

export function startShakeSensor(handler) {
  shake.start(handler);
  return () => shake.stop();
}
