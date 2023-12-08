import { API_CONFIG } from 'app-config';
import { DEV_MODE } from "./Utils";

const storage = DEV_MODE ? sessionStorage : localStorage;

function convertResponse(response) {
  if (response.ok) {
    return response.json();
  }
  throw new Error(`Request error: ${response.status}`);
}

export async function assign() {
  let id = storage.getItem('user-id');
  let path;
  if (!id || id === 'undefined') {
    path = 'assign';
  } else {
    path = `assign/${id}`;
  }
  const fetchOptions = {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', },
  };
  return fetch(
    `${API_CONFIG.game}/${path}`,
    { ...fetchOptions },
  )
    .then(convertResponse)
    .then((u) => {
      if (u.id) {
        storage.setItem('user-id', u.id);
      }
      return u;
    }).catch((e) => console.error(e));
}

export async function getState(runner) {
  const fetchOptions = {
    headers: { Accept: 'application/json' },
  };
  return fetch(
      `${API_CONFIG.game}/${runner}/state`,
      { ...fetchOptions },
  )
      .then(convertResponse)
      .then((u) => u)
      .catch((e) => console.error(e));
}



export function events(user, setState, reset) {
  if(!user) {
    return () => {};
  }
  const onEvent = (e) => {
    console.log(`=> Received game event: ${e.type}, ${JSON.stringify(e.data)}`);
    switch (e.type) {
      case 'START':
        reset();
        setState({ status: 'alive', data: e.data });
        break;
      case 'STOP':
        reset();
        setState({ status: 'off', data: e.data});
        break;
      case 'DEAD':
        setState({ status: 'dead', data: e.data});
        break;
      case 'SAVED':
        setState({ status: 'saved', data: e.data});
        break;
      case 'GAME_OVER':
        setState(p => ({ status: p.status, data: e.data}));
        break;
      case 'REASSIGN':
        window.location.reload();
        break;
      default:
        break;
    }
  };
  let stream;
  let i = 0;
  function connect() {
    console.log('Connecting to game event stream');
    stream = new EventSource(`${API_CONFIG.game}/${user.id}/events`);
    stream.onopen = () => {
      i = 0;
      console.log('Connected to game event stream');
      getState(user.id).then(setState);
    };
    stream.onmessage = (m) => onEvent(JSON.parse(m.data));
    stream.onerror = (e) => {
      console.error('Disconnecting from game event stream on error', e);
      stream.close();
      if (i > 0) {
        setState({ value: 'offline' });
      }
      if (i++ < 300) {
        setTimeout(connect, 2000);
      }
    };
  }
  connect();
  return () => {
    if (stream) {
      console.log('Disconnecting from game event stream');
      stream.close();
    }
  };
}
