import { writable } from 'svelte/store';

const seed = [
  {
    id: '1',
    user: { name: 'Ana Costa', username: 'anacosta', avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=ana&backgroundColor=7c3aed' },
    content: 'Acabei de lançar o meu primeiro projeto open source! Depois de meses a trabalhar nisto, finalmente está no GitHub. Feedback bem-vindo 🚀',
    image: null,
    likes: 38,
    comments: 7,
    liked: false,
    saved: false,
    time: '2m'
  },
  {
    id: '2',
    user: { name: 'Bruno Matos', username: 'brunomatos', avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=bruno&backgroundColor=2563eb' },
    content: 'SvelteKit é subestimado. Bundle size ridiculamente pequeno e a DX é incrível. Alguém mais aqui a usar?',
    image: null,
    likes: 91,
    comments: 23,
    liked: true,
    saved: false,
    time: '18m'
  },
  {
    id: '3',
    user: { name: 'Carla Reis', username: 'carlinha', avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=carla&backgroundColor=059669' },
    content: 'Morning run feita ✅ 8km em 42 minutos. Cada vez melhor!',
    image: 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=600&q=80',
    likes: 54,
    comments: 4,
    liked: false,
    saved: true,
    time: '1h'
  },
  {
    id: '4',
    user: { name: 'Diogo Ferreira', username: 'diogof', avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=diogo&backgroundColor=dc2626' },
    content: 'Reminder: descansar também faz parte do processo. Não é possível estar sempre no pico. Cuida de ti primeiro.',
    image: null,
    likes: 203,
    comments: 11,
    liked: false,
    saved: false,
    time: '3h'
  },
  {
    id: '5',
    user: { name: 'Elisa Nunes', username: 'elisanunes', avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=elisa&backgroundColor=d97706' },
    content: 'Lisboa de manhã cedo é uma cidade completamente diferente. Calma, fresca, quase só para nós 🌅',
    image: 'https://images.unsplash.com/photo-1555881400-74d7acaacd8b?w=600&q=80',
    likes: 176,
    comments: 19,
    liked: false,
    saved: false,
    time: '5h'
  }
];

export const posts = writable(seed);

export function toggleLike(id) {
  posts.update(all => all.map(p =>
    p.id === id
      ? { ...p, liked: !p.liked, likes: p.liked ? p.likes - 1 : p.likes + 1 }
      : p
  ));
}

export function toggleSave(id) {
  posts.update(all => all.map(p =>
    p.id === id ? { ...p, saved: !p.saved } : p
  ));
}

export function addPost(content) {
  posts.update(all => [{
    id: Date.now().toString(),
    user: { name: 'Alfred Silva', username: 'alfred', avatar: 'https://api.dicebear.com/8.x/notionists/svg?seed=alfred&backgroundColor=6d28d9' },
    content,
    image: null,
    likes: 0,
    comments: 0,
    liked: false,
    saved: false,
    time: 'agora'
  }, ...all]);
}
