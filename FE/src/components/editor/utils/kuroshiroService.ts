import Kuroshiro from 'kuroshiro';
import KuromojiAnalyzer from 'kuroshiro-analyzer-kuromoji';

class KuroshiroService {
  private kuroshiro: Kuroshiro | null = null;
  private initPromise: Promise<void> | null = null;
  private isReady = false;

  public async init(): Promise<void> {
    if (this.isReady) return;
    if (this.initPromise) return this.initPromise;

    this.initPromise = (async () => {
      try {
        const k = new Kuroshiro();
        // Use local dictionary in public/dict/ served by Vite/Web server
        const analyzer = new KuromojiAnalyzer({
          dictPath: '/dict/',
        });
        await k.init(analyzer);
        this.kuroshiro = k;
        this.isReady = true;
      } catch (err) {
        console.warn('Failed loading local /dict/, trying CDN fallback...', err);
        try {
          const k = new Kuroshiro();
          const analyzer = new KuromojiAnalyzer({
            dictPath: 'https://cdn.jsdelivr.net/npm/kuromoji@0.1.2/dict',
          });
          await k.init(analyzer);
          this.kuroshiro = k;
          this.isReady = true;
        } catch (cdnErr) {
          console.error('Failed to initialize Kuroshiro:', cdnErr);
          this.initPromise = null;
          throw cdnErr;
        }
      }
    })();

    return this.initPromise;
  }

  /**
   * Suggests Hiragana reading for Kanji or mixed Japanese text.
   * Runs 100% on the client without calling any server/Gemini APIs.
   */
  public async suggestReading(text: string): Promise<string> {
    if (!text || !text.trim()) return '';
    const trimmed = text.trim();

    try {
      await this.init();
      if (!this.kuroshiro) return '';
      const result = await this.kuroshiro.convert(trimmed, {
        to: 'hiragana',
        mode: 'normal',
      });
      return result || '';
    } catch (error) {
      console.error('Kuroshiro suggestReading error:', error);
      return '';
    }
  }

  public getStatus(): boolean {
    return this.isReady;
  }
}

export const kuroshiroService = new KuroshiroService();
