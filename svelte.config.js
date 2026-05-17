import adapter from '@sveltejs/adapter-static';

const config = {
  kit: {
    adapter: adapter({ out: 'build', fallback: '200.html' })
  }
};

export default config;