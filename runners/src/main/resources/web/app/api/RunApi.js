import { API_CONFIG } from "app-config";

export async function run(user, distance) {
  const fetchOptions = {
    method: 'POST',
    body: JSON.stringify({ runner: user.id, distance }),
    headers: { 'Content-Type': 'application/json' },
  };
  fetch(
      API_CONFIG.run,
    { ...fetchOptions },
  )
    .catch((e) => console.error(e));
}
