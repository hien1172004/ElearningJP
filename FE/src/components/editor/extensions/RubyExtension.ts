import { Node, mergeAttributes } from '@tiptap/core'

export interface RubyOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    ruby: {
      setRuby: (attrs: { base: string; reading: string }) => ReturnType
      unsetRuby: () => ReturnType
    }
  }
}

export const RubyExtension = Node.create<RubyOptions>({
  name: 'ruby',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,
  draggable: true,

  addOptions() {
    return {
      HTMLAttributes: {
        class: 'jlpt-ruby-node cursor-pointer select-none rounded hover:bg-indigo-50 transition px-0.5',
      },
    }
  },

  addAttributes() {
    return {
      base: {
        default: '',
        parseHTML: (element: HTMLElement) => {
          const clone = element.cloneNode(true) as HTMLElement
          const rt = clone.querySelector('rt')
          if (rt) rt.remove()
          return clone.textContent?.trim() || ''
        },
        renderHTML: (attributes) => ({
          'data-base': attributes.base,
        }),
      },
      reading: {
        default: '',
        parseHTML: (element: HTMLElement) => {
          const rt = element.querySelector('rt')
          return rt?.textContent?.trim() || ''
        },
        renderHTML: (attributes) => ({
          'data-reading': attributes.reading,
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'ruby',
      },
    ]
  },

  renderHTML({ node, HTMLAttributes }) {
    return [
      'ruby',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      node.attrs.base,
      ['rt', { class: 'text-[0.55em] text-indigo-500 font-sans' }, node.attrs.reading],
    ]
  },

  addCommands() {
    return {
      setRuby:
        (attrs) =>
        ({ commands }) => {
          return commands.insertContent({
            type: this.name,
            attrs,
          })
        },
      unsetRuby:
        () =>
        ({ commands, state }) => {
          const { selection } = state
          const node = state.doc.nodeAt(selection.from)
          if (node && node.type.name === 'ruby') {
            const baseText = node.attrs.base || ''
            return commands.insertContentAt(
              { from: selection.from, to: selection.to },
              baseText
            )
          }
          return false
        },
    }
  },

  addKeyboardShortcuts() {
    return {
      'Mod-Shift-r': () => {
        // Có thể emit event hoặc trigger modal
        return true
      },
    }
  },
})

export default RubyExtension
