import { Node, mergeAttributes } from '@tiptap/core'

export interface SpeakerOptions {
  HTMLAttributes: Record<string, any>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    speaker: {
      insertSpeaker: (label: string) => ReturnType
    }
  }
}

export const SpeakerExtension = Node.create<SpeakerOptions>({
  name: 'speaker',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addOptions() {
    return {
      HTMLAttributes: {
        class: 'inline-flex items-center px-1.5 py-0.2 mx-1 text-[11px] font-black rounded-md bg-indigo-100 text-indigo-900 border border-indigo-300 font-sans select-none',
      },
    }
  },

  addAttributes() {
    return {
      label: {
        default: '男',
        parseHTML: (element: HTMLElement) => {
          return element.getAttribute('data-speaker') || element.textContent?.replace(/[：:]/g, '') || '男'
        },
        renderHTML: (attributes) => ({
          'data-speaker': attributes.label,
        }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-speaker]',
      },
    ]
  },

  renderHTML({ node, HTMLAttributes }) {
    const label = node.attrs.label || '男'
    return [
      'span',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
      `${label}：`,
    ]
  },

  addCommands() {
    return {
      insertSpeaker:
        (label: string) =>
        ({ commands }) => {
          return commands.insertContent({
            type: this.name,
            attrs: { label },
          })
        },
    }
  },
})

export default SpeakerExtension
