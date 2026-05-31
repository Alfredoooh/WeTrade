package com.wilin.app.news

import com.wilin.app.ui.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object NewsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val HEADERS = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Safari/537.36",
        "Accept" to "application/rss+xml, application/xml, text/xml, */*",
        "Accept-Language" to "en-US,en;q=0.9,pt;q=0.8",
    )

    // ── Fontes por categoria ─────────────────────────────────────────────────

    private val SOURCES = mapOf(
        "world" to listOf(
            "https://www.aljazeera.com/xml/rss/all.xml",
            "https://feeds.skynews.com/feeds/rss/world.xml",
            "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
            "https://www.theguardian.com/world/rss",
            "http://rss.cnn.com/rss/edition_world.rss",
            "https://feeds.washingtonpost.com/rss/world",
            "https://www.independent.co.uk/news/world/rss",
            "https://abcnews.go.com/abcnews/internationalheadlines",
            "https://www.vox.com/rss/world-politics/index.xml",
            "https://thehill.com/homenews/feed/",
            "https://www.pbs.org/newshour/feeds/rss/world",
            "https://nypost.com/feed/",
            "https://rss.dw.com/rdf/rss-en-all",
            "https://www.france24.com/en/rss",
            "https://www.euronews.com/rss?level=theme&name=news",
            "https://feeds.npr.org/1001/rss.xml",
            "https://time.com/feed/",
            "https://www.usatoday.com/rss/news/",
            "https://feeds.nbcnews.com/nbcnews/public/news",
            "https://www.cbsnews.com/latest/rss/main",
            "https://feeds.foxnews.com/foxnews/world",
            "https://www.newsweek.com/rss",
            "https://rss.politico.com/politics-news.xml",
            "https://www.axios.com/feeds/feed.rss",
            "https://apnews.com/apf-topnews",
            "https://www.theatlantic.com/feed/all/",
            "https://foreignpolicy.com/feed/",
            "https://www.scmp.com/rss/91/feed",
            "https://www.straitstimes.com/news/world/rss.xml",
            "https://www.thenationalnews.com/rss",
            "https://www.trtworld.com/rss",
            "https://www.dawn.com/feeds/home",
            "https://www.hindustantimes.com/rss/world/rssfeed.xml",
            "https://www.thehindu.com/news/international/feeder/default.rss",
            "https://www.japantimes.co.jp/feed/",
            "https://www.abc.net.au/news/feed/51120/rss.xml",
            "https://www.themoscowtimes.com/rss/news",
            "https://www.rte.ie/news/rss/news-headlines.xml",
            "https://hongkongfp.com/feed/",
            "https://www.bangkokpost.com/rss/data/topstories.xml",
            "https://www.irishtimes.com/cmlink/news-1.1319492",
        ),
        "technology" to listOf(
            "https://www.theverge.com/rss/index.xml",
            "https://techcrunch.com/feed/",
            "https://www.wired.com/feed/rss",
            "http://feeds.arstechnica.com/arstechnica/index/",
            "https://www.engadget.com/rss.xml",
            "https://www.cnet.com/rss/all/",
            "https://venturebeat.com/feed/",
            "https://www.theregister.com/headlines.atom",
            "https://gizmodo.com/rss",
            "https://mashable.com/feed/",
            "https://www.zdnet.com/news/rss.xml",
            "https://www.digitaltrends.com/feed/",
            "https://www.techradar.com/rss",
            "https://thenextweb.com/feed/",
            "https://www.pcmag.com/rss/news",
            "https://www.tomshardware.com/feeds/all",
            "https://www.androidpolice.com/feed/",
            "https://9to5mac.com/feed/",
            "https://9to5google.com/feed/",
            "https://appleinsider.com/rss/news/",
            "https://www.macrumors.com/macrumors.xml",
            "https://news.ycombinator.com/rss",
            "https://www.technologyreview.com/feed/",
            "https://www.gsmarena.com/rss-news-reviews.php3",
            "https://www.xda-developers.com/feed/",
            "https://www.techspot.com/backend.xml",
            "https://www.bleepingcomputer.com/feed/",
            "https://feeds.feedburner.com/TheHackersNews",
            "https://www.darkreading.com/rss.xml",
            "https://www.tecnoblog.net/feed/",
            "https://olhardigital.com.br/feed/",
            "https://canaltech.com.br/rss/",
            "https://www.androidauthority.com/feed/",
            "https://www.artificialintelligence-news.com/feed/",
            "https://spectrum.ieee.org/feeds/feed.rss",
            "https://www.infoworld.com/index.rss",
            "https://www.computerworld.com/index.rss",
            "https://siliconangle.com/feed/",
            "https://www.techrepublic.com/rssfeeds/articles/",
            "https://www.windowscentral.com/rss.xml",
            "https://www.notebookcheck.net/News.49.0.html?id=49&type=rss",
            "https://www.anandtech.com/rss/",
            "https://readwrite.com/feed/",
        ),
        "science" to listOf(
            "https://www.sciencedaily.com/rss/all.xml",
            "https://www.nasa.gov/news-release/feed/",
            "https://www.newscientist.com/feed/home/",
            "http://feeds.arstechnica.com/arstechnica/science/",
            "https://www.space.com/feeds.xml",
            "https://www.wired.com/category/science/feed",
            "https://www.theguardian.com/science/rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/Science.xml",
            "https://universetoday.com/feed",
            "https://www.nature.com/nature.rss",
            "https://phys.org/rss-feed/",
            "https://futurism.com/feed",
            "https://www.livescience.com/feeds/all",
            "https://www.discovermagazine.com/rss",
            "https://earthsky.org/feed/",
            "https://www.sciencenews.org/feed",
            "https://www.smithsonianmag.com/rss/latest_articles/",
            "https://www.popsci.com/feed/",
            "https://www.quantamagazine.org/feed/",
            "https://www.iflscience.com/rss.xml",
            "https://www.sciencealert.com/feed",
            "https://neurosciencenews.com/feed/",
            "https://www.eurekalert.org/rss.xml",
            "https://www.esa.int/rssfeed/ESA_Top_News",
            "https://www.zmescience.com/feed/",
            "https://journals.plos.org/plosone/feed/atom",
            "https://www.frontiersin.org/journals/science/rss",
        ),
        "health" to listOf(
            "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml",
            "https://www.theguardian.com/society/health/rss",
            "https://feeds.skynews.com/feeds/rss/health.xml",
            "https://www.who.int/feeds/entity/news/en/rss.xml",
            "https://www.sciencedaily.com/rss/health_medicine/",
            "https://www.medicalnewstoday.com/rss/medicalnewstoday",
            "https://www.healthline.com/rss/health-news",
            "https://www.webmd.com/rss/rss.aspx?RSSSource=RSS_PUBLIC",
            "https://www.statnews.com/feed/",
            "https://www.livescience.com/feeds/health",
            "https://rss.nytimes.com/services/xml/rss/nyt/Well.xml",
            "https://www.cnn.com/services/rss/health.rss",
            "https://psychcentral.com/feed/",
            "https://www.verywellmind.com/feed",
            "https://www.verywellhealth.com/feed",
            "https://www.mayoclinic.org/rss/all-health-information-topics",
            "https://www.nih.gov/news-events/news-releases/feed",
            "https://www.hsph.harvard.edu/news/feed/",
            "https://www.thelancet.com/rssfeed/lancet_online.xml",
            "https://www.medpagetoday.com/rss/headlines.xml",
            "https://examine.com/feed.xml",
            "https://neurosciencenews.com/feed/",
            "https://www.healio.com/rss/cardiology",
            "https://www.healio.com/rss/oncology",
        ),
        "sports" to listOf(
            "https://www.espn.com/espn/rss/news",
            "https://www.skysports.com/rss/12040",
            "https://www.theguardian.com/sport/rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml",
            "https://www.cbssports.com/rss/headlines/",
            "https://www.marca.com/rss/portada.xml",
            "https://www.eurosport.com/rss/news/",
            "https://www.goal.com/feeds/en/news",
            "https://www.formula1.com/content/fom-website/en/latest/all.xml",
            "https://theathletic.com/rss/news/",
            "https://www.bleacherreport.com/articles/feed",
            "https://www.sportingnews.com/rss",
            "https://www.nba.com/rss/nba_rss.xml",
            "https://www.si.com/rss/si_topstories.rss",
            "https://sportbible.com/feed",
            "https://www.90min.com/feed",
            "https://www.givemesport.com/feed/",
            "https://www.record.pt/rss/",
            "https://www.abola.pt/rss/",
            "https://www.maisfutebol.iol.pt/rss",
            "https://www.mlb.com/feeds/news/rss.xml",
            "https://www.ufc.com/rss.xml",
            "https://www.insidethegames.biz/rss",
            "https://www.cyclingnews.com/rss",
            "https://www.motorsport.com/rss/f1/news/",
            "https://www.autosport.com/rss/f1/news/",
            "https://racer.com/feed/",
            "https://www.nhl.com/rss/news.xml",
            "https://www.rugbyworld.com/feed",
            "https://www.cricbuzz.com/rss-feeds/index.xml",
            "https://www.golf.com/feed/",
            "https://www.totalsportek.com/feed/",
            "https://boxingnewsonline.net/feed/",
            "https://www.essentiallysports.com/feed/",
            "https://www.transfermarkt.com/rss/transfers.rss",
        ),
        "entertainment" to listOf(
            "https://feeds.skynews.com/feeds/rss/entertainment.xml",
            "https://variety.com/feed/",
            "https://www.hollywoodreporter.com/t/news/feed/",
            "https://deadline.com/feed/",
            "https://pitchfork.com/rss/news/feed/r.xml",
            "https://www.rollingstone.com/music/music-news/feed/",
            "https://ew.com/feed/",
            "https://www.theguardian.com/culture/rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/Arts.xml",
            "https://www.billboard.com/feed/",
            "https://www.nme.com/feed",
            "https://www.ign.com/rss/articles",
            "https://kotaku.com/rss",
            "https://www.vulture.com/rss/all.xml",
            "https://collider.com/feed/",
            "https://screenrant.com/feed/",
            "https://www.indiewire.com/feed/",
            "https://pagesix.com/feed/",
            "https://www.cinemablend.com/rss/news",
            "https://www.slashfilm.com/feed/",
            "https://www.digitalspy.com/feeds/rss/",
            "https://www.tvline.com/feed/",
            "https://www.animenewsnetwork.com/news/rss.xml",
            "https://people.com/rss/all/",
            "https://www.tmz.com/rss.xml",
            "https://www.usmagazine.com/rss",
            "https://comingsoon.net/feed",
            "https://bloody-disgusting.com/feed/",
            "https://www.rottentomatoes.com/rss/movies_in_theaters.rss",
            "https://www.empireonline.com/movies/rss/",
            "https://www.denofgeek.com/feed/",
            "https://www.avclub.com/rss",
        ),
        "business" to listOf(
            "https://rss.nytimes.com/services/xml/rss/nyt/Business.xml",
            "https://feeds.skynews.com/feeds/rss/business.xml",
            "https://www.theguardian.com/business/rss",
            "https://www.marketwatch.com/rss/topstories",
            "https://www.cnbc.com/id/100003114/device/rss/rss.html",
            "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
            "https://www.economist.com/latest/rss.xml",
            "https://feeds.bloomberg.com/markets/news.rss",
            "https://www.forbes.com/business/feed/",
            "https://www.businessinsider.com/rss",
            "https://feeds.washingtonpost.com/rss/business",
            "https://fortune.com/feed",
            "https://hbr.org/resources/rss/editorial/feed",
            "https://www.fastcompany.com/latest/rss",
            "https://www.inc.com/rss",
            "https://www.entrepreneur.com/latest.rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/Economy.xml",
            "https://www.thestreet.com/rss/index.xml",
            "https://www.reuters.com/rssFeed/businessNews",
            "https://www.visualcapitalist.com/feed/",
            "https://www.wsj.com/xml/rss/3_7085.xml",
            "https://www.livemint.com/rss/news",
            "https://economictimes.indiatimes.com/rssfeedstopstories.cms",
            "https://www.smh.com.au/rss/business.xml",
            "https://www.businesstimes.com.sg/rss",
        ),
        "africa" to listOf(
            "https://allafrica.com/tools/headlines/rdf/africa/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/angola/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/southafrica/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/nigeria/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/kenya/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/ethiopia/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/ghana/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/egypt/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/mozambique/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/zimbabwe/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/drc/headlines.rdf",
            "https://rss.dw.com/rdf/rss-en-africa",
            "https://www.france24.com/en/africa/rss",
            "https://www.theafricareport.com/feed/",
            "https://www.africanews.com/feed/rss",
            "https://mg.co.za/feed/",
            "https://www.dailymaverick.co.za/feed/",
            "https://punchng.com/feed/",
            "https://www.premiumtimesng.com/feed/",
            "https://www.nation.co.ke/rss/",
            "https://angop.ao/angola/pt_pt/noticias/rss_noticias.xml",
            "https://www.voaportugues.com/api/zmoqmmveii",
            "https://rr.sapo.pt/rss/ultimas",
            "https://sicnoticias.pt/rss",
            "https://www.dn.pt/rss/mundo.xml",
            "https://www.publico.pt/api/rss/mundo",
            "https://www.businessdayonline.com/feed/",
            "https://www.thecitizen.co.tz/tanzania/news/rss",
            "https://www.monitor.co.ug/rss",
            "https://www.newzimbabwe.com/feed/",
            "https://allafrica.com/tools/headlines/rdf/cameroon/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/senegal/headlines.rdf",
            "https://allafrica.com/tools/headlines/rdf/rwanda/headlines.rdf",
        ),
        "politics" to listOf(
            "https://rss.politico.com/politics-news.xml",
            "https://thehill.com/homenews/feed/",
            "https://www.vox.com/rss/politics/index.xml",
            "https://feeds.washingtonpost.com/rss/politics",
            "https://rss.nytimes.com/services/xml/rss/nyt/Politics.xml",
            "https://www.axios.com/feeds/feed.rss",
            "https://www.realclearpolitics.com/index.xml",
            "https://www.npr.org/rss/rss.php?id=1014",
            "https://slate.com/feeds/all.rss",
            "https://www.nationalreview.com/feed/",
            "https://feeds.foxnews.com/foxnews/politics",
            "https://reason.com/feed/",
            "https://foreignpolicy.com/feed/",
            "https://www.foreignaffairs.com/rss.xml",
            "https://www.brookings.edu/feed/",
            "https://apnews.com/apf-politics",
            "https://www.theguardian.com/us-news/us-politics/rss",
            "https://www.euractiv.com/sections/politics/feed/",
            "https://www.politico.eu/feed/",
            "https://www.france24.com/en/europe/rss",
            "https://www.publico.pt/api/rss/politica",
            "https://www.expresso.pt/rss",
            "https://www.thedailybeast.com/rss.xml",
            "https://www.motherjones.com/feed/",
            "https://talkingpointsmemo.com/feed/",
            "https://crooksandliars.com/rss.xml",
        ),
        "gaming" to listOf(
            "https://www.ign.com/rss/articles",
            "https://www.gamespot.com/feeds/mashup/",
            "https://kotaku.com/rss",
            "https://www.polygon.com/rss/index.xml",
            "https://www.pcgamer.com/rss/",
            "https://www.eurogamer.net/feed",
            "https://www.rockpapershotgun.com/feed/rss",
            "https://www.vg247.com/feed/rss",
            "https://www.gamesradar.com/feeds/rss",
            "https://www.pushsquare.com/feeds/latest",
            "https://www.nintendolife.com/feeds/latest",
            "https://www.destructoid.com/feed/",
            "https://www.shacknews.com/rss",
            "https://www.dualshockers.com/feed/",
            "https://toucharcade.com/feed/",
            "https://gamerant.com/feed/",
            "https://www.thegamer.com/feed/",
            "https://dotesports.com/feed",
            "https://www.pcgamesn.com/mainrss.xml",
            "https://www.wccftech.com/feed/",
            "https://www.siliconera.com/feed/",
            "https://esportsinsider.com/feed/",
            "https://www.gamedeveloper.com/rss.xml",
            "https://www.gamesindustry.biz/rss",
            "https://www.pocketgamer.com/rss/news/",
            "https://www.gameinformer.com/rss.xml",
            "https://metro.co.uk/tag/gaming/feed/",
            "https://www.rpgsite.net/rss.xml",
        ),
        "finance" to listOf(
            "https://feeds.bloomberg.com/markets/news.rss",
            "https://feeds.a.dj.com/rss/RSSMarketsMain.xml",
            "https://www.marketwatch.com/rss/topstories",
            "https://www.cnbc.com/id/10000664/device/rss/rss.html",
            "https://seekingalpha.com/feed.xml",
            "https://www.thestreet.com/rss/index.xml",
            "https://www.investopedia.com/feedbuilder/feed/getfeed/?feedName=rss_headline",
            "https://rss.nytimes.com/services/xml/rss/nyt/Economy.xml",
            "https://finance.yahoo.com/rss/topfinstories",
            "https://www.nasdaq.com/feed/rssoutbound?category=Markets",
            "https://www.benzinga.com/feed",
            "https://www.coindesk.com/arc/outboundfeeds/rss/",
            "https://cointelegraph.com/rss",
            "https://decrypt.co/feed",
            "https://beincrypto.com/feed/",
            "https://bitcoinmagazine.com/feed",
            "https://exame.com/rss/",
            "https://www.infomoney.com.br/feed/",
            "https://www.fool.com/feeds/index.aspx",
            "https://www.valuewalk.com/feed/",
            "https://www.cryptonews.com/news/feed/",",
            "https://cryptopotato.com/feed/",
            "https://ambcrypto.com/feed/",
            "https://www.newsbtc.com/feed/",
            "https://cryptoslate.com/feed/",
        ),
        "environment" to listOf(
            "https://www.theguardian.com/environment/rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/Environment.xml",
            "https://www.ecowatch.com/rss",
            "https://e360.yale.edu/feed",
            "https://www.climatecentral.org/feed",
            "https://insideclimatenews.org/feed/",
            "https://www.carbonbrief.org/feed/",
            "https://www.treehugger.com/feeds/all/",
            "https://www.nationalgeographic.com/environment/rss",
            "https://news.mongabay.com/feed/",
            "https://cleantechnica.com/feed/",
            "https://electrek.co/feed/",
            "https://phys.org/rss-feed/earth-climate-news/",
            "https://www.anthropocenemagazine.org/feed/",
            "https://www.renewableenergyworld.com/feed/",
            "https://www.solarpowerworldonline.com/feed/",
            "https://www.climatechangenews.com/feed/",
            "https://www.iucn.org/rss.xml",
            "https://www.greenbiz.com/feeds/news",
            "https://www.nrdc.org/rss.xml",
        ),
        "travel" to listOf(
            "https://www.lonelyplanet.com/news/feed",
            "https://www.travelandleisure.com/rss",
            "https://www.cntraveler.com/feed/rss",
            "https://www.nomadicmatt.com/feed/",
            "https://www.thepointsguy.com/feed/",
            "https://onemileatatime.com/feed/",
            "https://www.smartertravel.com/feed/",
            "https://www.atlasobscura.com/feeds/latest",
            "https://www.timeout.com/travel/rss",
            "https://www.telegraph.co.uk/travel/rss",
            "https://www.independent.co.uk/travel/rss",
            "https://www.theguardian.com/travel/rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/Travel.xml",
            "https://www.nationalgeographic.com/travel/rss",
            "https://www.cnn.com/travel/rss",
            "https://www.forbes.com/travel/feed/",
            "https://skift.com/feed/",
            "https://roadsandkingdoms.com/feed/",
        ),
        "food" to listOf(
            "https://www.seriouseats.com/atom.xml",
            "https://www.epicurious.com/feed/news-articles-rss",
            "https://www.bonappetit.com/feed/rss",
            "https://www.tastingtable.com/feed/",
            "https://www.foodandwine.com/syndication/rss/",
            "https://www.saveur.com/feed/",
            "https://www.eater.com/rss/index.xml",
            "https://www.thekitchn.com/main.rss",
            "https://www.simplyrecipes.com/feed/",
            "https://minimalistbaker.com/feed/",
            "https://www.halfbakedharvest.com/feed/",
            "https://www.theguardian.com/lifeandstyle/food-and-drink/rss",
            "https://rss.nytimes.com/services/xml/rss/nyt/DiningandWine.xml",
            "https://www.healthline.com/rss/nutrition",
            "https://www.vinepair.com/feed/",
            "https://www.bbcgoodfood.com/api/content-service/rss/all",
            "https://www.delish.com/rss/all.xml/",
            "https://www.allrecipes.com/feeds/allrecipes-news.rss",
            "https://www.tasteofhome.com/feed/",
        ),
    )

    // ── Cache em memória (2 min TTL) ─────────────────────────────────────────
    private val memCache = mutableMapOf<String, Pair<Long, List<NewsItem>>>()
    private const val MEM_TTL = 2 * 60 * 1000L

    // ── Fetch principal ──────────────────────────────────────────────────────
    suspend fun fetchList(category: String, limit: Int = 40): List<NewsItem> =
        withContext(Dispatchers.IO) {
            val cached = memCache[category]
            if (cached != null && System.currentTimeMillis() - cached.first < MEM_TTL) {
                return@withContext cached.second.take(limit)
            }

            val feeds = SOURCES[category] ?: SOURCES["world"]!!
            val results = feeds.map { url ->
                async {
                    try { parseFeed(url, category) } catch (e: Exception) { emptyList() }
                }
            }.awaitAll().flatten()

            val deduped = dedup(results)
                .sortedByDescending { parsePubTs(it.publishedAt) }

            val enriched = enrichImages(deduped)
            memCache[category] = Pair(System.currentTimeMillis(), enriched)
            enriched.take(limit)
        }

    // ── Detalhe do artigo ────────────────────────────────────────────────────
    suspend fun fetchDetail(url: String): NewsDetailItem =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).headers(buildHeaders()).build()
                val html = client.newCall(req).execute().body?.string() ?: return@withContext NewsDetailItem()
                val doc = Jsoup.parse(html)

                val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
                    ?: doc.selectFirst("h1")?.text() ?: ""

                val desc = doc.selectFirst("meta[property=og:description]")?.attr("content")
                    ?: doc.selectFirst("meta[name=description]")?.attr("content") ?: ""

                val image = listOf(
                    "meta[property=og:image]",
                    "meta[property=og:image:url]",
                    "meta[name=twitter:image]",
                    "meta[name=twitter:image:src]"
                ).firstNotNullOfOrNull { sel ->
                    doc.selectFirst(sel)?.attr("content")?.takeIf { isValidImage(it) }
                } ?: ""

                val author = doc.selectFirst("meta[name=author]")?.attr("content")
                    ?: doc.selectFirst("meta[property=article:author]")?.attr("content") ?: ""

                val published = doc.selectFirst("meta[property=article:published_time]")?.attr("content")
                    ?: doc.selectFirst("time")?.attr("datetime") ?: ""

                val body = scrapeBody(doc)

                NewsDetailItem(
                    title       = cleanText(title, 300),
                    body        = body.ifEmpty { cleanText(desc, 800) },
                    description = cleanText(desc, 800),
                    imageUrl    = image,
                    author      = cleanText(author, 100),
                    publishedAt = published,
                    sourceName  = getDomain(url).split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "",
                    sourceUrl   = url,
                )
            } catch (e: Exception) {
                NewsDetailItem()
            }
        }

    // ── RSS parser ───────────────────────────────────────────────────────────
    private fun parseFeed(feedUrl: String, category: String): List<NewsItem> {
        val req = Request.Builder().url(feedUrl).headers(buildHeaders()).build()
        val body = client.newCall(req).execute().body?.string() ?: return emptyList()
        val doc = Jsoup.parse(body, "", Parser.xmlParser())
        val items = doc.select("item, entry")
        return items.take(25).mapNotNull { item ->
            val title = item.selectFirst("title")?.text()?.trim() ?: return@mapNotNull null
            if (title.length < 5) return@mapNotNull null

            val url = item.selectFirst("link")?.let {
                it.attr("href").ifEmpty { it.text().trim() }
            } ?: item.selectFirst("guid")?.text()?.takeIf { it.startsWith("http") }
            ?: return@mapNotNull null

            val desc = item.selectFirst("description, summary, content\\:encoded, content")
                ?.text()?.let { cleanText(it, 500) } ?: ""

            val img = extractImage(item, desc)

            val pubDate = listOf("pubDate", "dc\\:date", "published", "updated")
                .firstNotNullOfOrNull { item.selectFirst(it)?.text()?.trim()?.takeIf { t -> t.isNotEmpty() } } ?: ""

            val author = listOf("dc\\:creator", "author", "dc\\:author")
                .firstNotNullOfOrNull { item.selectFirst(it)?.text()?.trim()?.takeIf { t -> t.isNotEmpty() } } ?: ""

            val domain = getDomain(url)

            NewsItem(
                title       = cleanText(title, 300),
                description = desc,
                imageUrl    = img,
                sourceUrl   = url,
                sourceName  = domain.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "",
                faviconUrl  = "https://www.google.com/s2/favicons?domain=$domain&sz=128",
                category    = category,
                body        = desc,
                author      = author,
                publishedAt = pubDate,
            )
        }
    }

    // ── Extrai imagem do item RSS ─────────────────────────────────────────────
    private fun extractImage(item: org.jsoup.nodes.Element, desc: String): String {
        // media:content / media:thumbnail
        for (tag in listOf("media:content", "media:thumbnail")) {
            val best = item.select(tag)
                .filter { it.attr("type").let { t -> t.isEmpty() || t.startsWith("image") } }
                .maxByOrNull { it.attr("width").toIntOrNull() ?: 1 }
            val url = best?.attr("url") ?: best?.attr("src") ?: ""
            if (isValidImage(url)) return url
        }
        // enclosure
        item.selectFirst("enclosure")?.let {
            val eu = it.attr("url"); val et = it.attr("type")
            if (isValidImage(eu) && (et.isEmpty() || et.startsWith("image"))) return eu
        }
        // img dentro da descrição
        val m = Regex("""<img[^>]+src=["']([^"']+)["']""").find(desc)
        if (m != null && isValidImage(m.groupValues[1])) return m.groupValues[1]

        return ""
    }

    // ── Enriquece artigos sem imagem via OG scraping ──────────────────────────
    private suspend fun enrichImages(articles: List<NewsItem>): List<NewsItem> =
        withContext(Dispatchers.IO) {
            val noImg = articles.filter { it.imageUrl.isEmpty() }.take(25)
            val fetched = noImg.map { art ->
                async {
                    try {
                        val req = Request.Builder().url(art.sourceUrl).headers(buildHeaders()).build()
                        val html = client.newCall(req).execute().body?.string() ?: return@async art
                        val doc = Jsoup.parse(html)
                        val img = listOf(
                            "meta[property=og:image]",
                            "meta[property=og:image:url]",
                            "meta[name=twitter:image]",
                        ).firstNotNullOfOrNull { sel ->
                            doc.selectFirst(sel)?.attr("content")?.takeIf { isValidImage(it) }
                        } ?: ""
                        if (img.isNotEmpty()) art.copy(imageUrl = img) else art
                    } catch (e: Exception) { art }
                }
            }.awaitAll()

            val enrichedMap = fetched.associateBy { it.sourceUrl }
            articles.map { enrichedMap[it.sourceUrl] ?: it }
        }

    // ── Scrape body ──────────────────────────────────────────────────────────
    private fun scrapeBody(doc: org.jsoup.nodes.Document): String {
        for (bad in listOf("script", "style", "nav", "aside", "figure",
            "figcaption", "iframe", "button", "form", "header", "footer", "noscript")) {
            doc.select(bad).remove()
        }
        val container = doc.selectFirst(
            "article, [class*=article-body], [class*=post-content], [class*=entry-content], " +
            "[class*=story-body], [class*=article__body], [class*=content-body], " +
            "[class*=main-content], [id*=article], [id*=content], main"
        )
        val paragraphs = (container ?: doc).select("p")
            .filter { it.text().trim().length > 40 }
            .joinToString(" ") { cleanText(it.text(), 1000) }
        return paragraphs.take(5000)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun isValidImage(url: String): Boolean {
        if (url.isEmpty() || !url.startsWith("http")) return false
        val u = url.lowercase().split("?")[0]
        val badWords = listOf("pixel","tracker","tracking","beacon","1x1","spacer",
            "logo","favicon","icon","avatar","placeholder","blank","transparent",
            "badge","button","spinner","loading","bbc")
        if (badWords.any { u.contains(it) }) return false
        val badExt = listOf(".gif",".ico",".svg",".bmp",".tiff",".txt",".js",".css")
        if (badExt.any { u.endsWith(it) }) return false
        return true
    }

    private fun cleanText(text: String, max: Int): String {
        return text
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&(?:[a-zA-Z]+|#\\d+);"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(max)
    }

    private fun getDomain(url: String): String = try {
        java.net.URI(url).host?.removePrefix("www.") ?: ""
    } catch (e: Exception) { "" }

    private fun makeId(title: String, url: String): String {
        val input = title.trim().lowercase() + url.trim()
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    private fun dedup(articles: List<NewsItem>): List<NewsItem> {
        val seen = mutableSetOf<String>()
        return articles.filter { a ->
            val key = a.title.take(80).lowercase().replace(Regex("\\W"), "")
            seen.add(key)
        }
    }

    private fun parsePubTs(pub: String): Long {
        if (pub.isEmpty()) return 0L
        return try {
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH)
                .parse(pub)?.time ?: 0L
        } catch (e: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.ENGLISH)
                    .parse(pub)?.time ?: 0L
            } catch (e2: Exception) { 0L }
        }
    }

    private fun buildHeaders() = okhttp3.Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.9,pt;q=0.8")
        .build()
}

data class NewsDetailItem(
    val title       : String = "",
    val body        : String = "",
    val description : String = "",
    val imageUrl    : String = "",
    val author      : String = "",
    val publishedAt : String = "",
    val sourceName  : String = "",
    val sourceUrl   : String = "",
)