const RSS_FEEDS = [
  'https://www.record.pt/rss',
  'https://www.abola.pt/rss/index.aspx',
  'https://feeds.bbci.co.uk/sport/football/rss.xml',
  'https://www.goal.com/feeds/en/news',
];

const PROXY = 'https://api.allorigins.win/get?url=';

async function fetchRSS(url) {
  try {
    const res = await fetch(`${PROXY}${encodeURIComponent(url)}`);
    const data = await res.json();
    const parser = new DOMParser();
    const xml = parser.parseFromString(data.contents, 'text/xml');
    const items = Array.from(xml.querySelectorAll('item'));
    return items.map(item => ({
      title: item.querySelector('title')?.textContent || '',
      link: item.querySelector('link')?.textContent || '',
      description: item.querySelector('description')?.textContent?.replace(/<[^>]*>/g, '').slice(0, 200) || '',
      pubDate: item.querySelector('pubDate')?.textContent || '',
      image: item.querySelector('enclosure')?.getAttribute('url') || item.querySelector('thumbnail')?.getAttribute('url') || null,
      source: new URL(url).hostname.replace('www.', ''),
    }));
  } catch (e) {
    return [];
  }
}

export async function getFootballNews() {
  const results = await Promise.all(RSS_FEEDS.map(fetchRSS));
  return results
    .flat()
    .filter(item => item.title)
    .sort((a, b) => new Date(b.pubDate) - new Date(a.pubDate))
    .slice(0, 30);
}