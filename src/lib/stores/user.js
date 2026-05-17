import { writable } from 'svelte/store';

export const currentUser = writable({
  id: '1',
  name: 'Alfred Silva',
  username: 'alfred',
  avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=alfred&backgroundColor=6d28d9',
  bio: 'Builder de apps 🛠️',
  followers: 412,
  following: 89
});
