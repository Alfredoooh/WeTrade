const NEWS_KEY = 'pub_7d7d1ac2f86b4bc6b4662fd5d6dad47c';

export async function getFootballNews(page = 1) {
  try {
    const res = await fetch(
      `https://newsdata.io/api/1/news?apikey=${NEWS_KEY}&q=futebol+football&language=pt,en&category=sports&size=10`
    );
    const data = await res.json();
    return data?.results || [];
  } catch (e) {
    return [];
  }
}