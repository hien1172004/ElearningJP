declare module 'hanzi-writer' {
  export interface HanziWriterOptions {
    width?: number
    height?: number
    padding?: number
    showOutline?: boolean
    strokeAnimationSpeed?: number
    strokeHighlightSpeed?: number
    strokeFadeDuration?: number
    delayBetweenStrokes?: number
    strokeColor?: string
    outlineColor?: string
    highlightColor?: string
    drawingColor?: string
    drawingWidth?: number
    showCharacter?: boolean
    showHintAfterMisses?: number
    highlightOnComplete?: boolean
    markStrokeCorrectAfterMisses?: boolean
    quizStartStrokeNum?: number
    charDataLoader?: (char: string, onLoaded: (data: any) => void, onError: (err: any) => void) => void
    onLoadCharDataError?: (err: any) => void
  }

  export interface QuizOptions {
    onMistake?: (strokeData: {
      strokeNum: number
      mistakesOnStroke: number
      totalMistakes: number
      strokesRemaining: number
      drawnPath: { points: Array<{ x: number; y: number }> }
    }) => void
    onCorrectStroke?: (strokeData: {
      strokeNum: number
      mistakesOnStroke: number
      totalMistakes: number
      strokesRemaining: number
      drawnPath: { points: Array<{ x: number; y: number }> }
    }) => void
    onComplete?: (summary: {
      character: string
      totalMistakes: number
    }) => void
    showOutline?: boolean
    showHintAfterMisses?: number
  }

  export default class HanziWriter {
    static create(
      element: HTMLElement | string,
      character: string,
      options?: HanziWriterOptions
    ): HanziWriter

    static loadCharacterData(
      character: string,
      options?: { charDataLoader?: HanziWriterOptions['charDataLoader'] }
    ): Promise<any>

    animateCharacter(options?: { onComplete?: () => void }): Promise<void>
    animateStroke(strokeNum: number, options?: { onComplete?: () => void }): Promise<void>
    loopCharacterAnimation(): void
    pauseAnimation(): void
    resumeAnimation(): void
    showOutline(options?: { duration?: number }): Promise<void>
    hideOutline(options?: { duration?: number }): Promise<void>
    showCharacter(options?: { duration?: number }): Promise<void>
    hideCharacter(options?: { duration?: number }): Promise<void>
    quiz(options?: QuizOptions): void
    cancelQuiz(): void
    setCharacter(character: string): Promise<void>
    destroy(): void
  }
}
