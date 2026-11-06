import React, { useMemo } from 'react'
import { Modal, Tooltip } from 'antd'
import type { KanjiCompGraph, KanjiComponentDetail } from '@/types'

interface KanjiBreakdownModalProps {
  open: boolean
  character: string
  hanViet?: string
  meaningVi?: string
  compGraph?: KanjiCompGraph
  compDetails?: KanjiComponentDetail[]
  onClose: () => void
  onSelectCharacter?: (char: string) => void
}

interface PositionedNode {
  id: string
  word: string
  x: number
  y: number
  isRoot: boolean
  hanViet?: string | null
  detail?: string | null
}

interface PositionedLink {
  sourceId: string
  targetId: string
  x1: number
  y1: number
  x2: number
  y2: number
  midX: number
  midY: number
}

export const KanjiBreakdownModal: React.FC<KanjiBreakdownModalProps> = ({
  open,
  character,
  hanViet,
  meaningVi,
  compGraph,
  compDetails,
  onClose,
  onSelectCharacter,
}) => {
  // Map Han-Viet by component character
  const hanVietMap = useMemo(() => {
    const map: Record<string, string> = {}
    if (compDetails) {
      for (const c of compDetails) {
        if (c.w && c.h) {
          map[c.w] = c.h
        }
      }
    }
    return map
  }, [compDetails])

  // Tính toán vị trí (x, y) của các nodes và links theo thuật toán phân cấp cây không chồng đè
  const { nodes, links, viewBox } = useMemo(() => {
    const rawLinks = compGraph?.links || []
    const rawNodes = compGraph?.nodes || []

    if (!character || (rawLinks.length === 0 && rawNodes.length === 0)) {
      // Trường hợp chữ đơn lẻ không có liên kết
      return {
        nodes: [
          {
            id: character,
            word: character,
            x: 250,
            y: 150,
            isRoot: true,
            hanViet: hanViet || hanVietMap[character],
            detail: meaningVi,
          },
        ],
        links: [],
        viewBox: '0 0 500 300',
      }
    }

    const rawNodesMap: Record<string, { word: string; hanViet?: string; detail?: string }> = {}
    for (const n of rawNodes) {
      rawNodesMap[n.id] = {
        word: n.word,
        hanViet: n.hanViet,
        detail: n.detail,
      }
    }

    const compDetailsMap: Record<string, { h?: string | null; d?: string | null }> = {}
    if (compDetails) {
      for (const c of compDetails) {
        if (c.w) {
          compDetailsMap[c.w] = { h: c.h, d: c.d }
        }
      }
    }

    // Xây dựng đồ thị kề (adjacency list)
    const adj: Record<string, string[]> = {}
    for (const l of rawLinks) {
      if (!adj[l.source]) adj[l.source] = []
      adj[l.source].push(l.target)
    }

    const posMap: Record<string, { x: number; y: number }> = {}
    let leafCount = 0

    // Đệ quy tính toán tọa độ x, y
    const layoutNode = (nodeId: string, depth: number): number => {
      const children = adj[nodeId] || []
      const y = 80 + depth * 110

      if (children.length === 0) {
        const x = 70 + leafCount * 110
        leafCount += 1
        posMap[nodeId] = { x, y }
        return x
      }

      const childXs = children.map((ch) => layoutNode(ch, depth + 1))
      const x = childXs.reduce((a, b) => a + b, 0) / childXs.length
      posMap[nodeId] = { x, y }
      return x
    }

    layoutNode(character, 0)

    // Tạo danh sách nodes có tọa độ
    const positionedNodes: PositionedNode[] = []
    const positionedLinks: PositionedLink[] = []

    let minX = Infinity
    let maxX = -Infinity
    let minY = Infinity
    let maxY = -Infinity

    for (const [nid, pos] of Object.entries(posMap)) {
      const isRoot = nid === character
      const rawNode = rawNodesMap[nid]
      const word = isRoot ? character : (rawNode?.word || nid)
      const nodeHanViet = isRoot
        ? (hanViet || hanVietMap[word])
        : (rawNode?.hanViet || compDetailsMap[word]?.h || hanVietMap[word])
      const nodeDetail = isRoot
        ? (meaningVi || rawNode?.detail)
        : (rawNode?.detail || compDetailsMap[word]?.d)

      positionedNodes.push({
        id: nid,
        word,
        x: pos.x,
        y: pos.y,
        isRoot,
        hanViet: nodeHanViet,
        detail: nodeDetail,
      })

      if (pos.x < minX) minX = pos.x
      if (pos.x > maxX) maxX = pos.x
      if (pos.y < minY) minY = pos.y
      if (pos.y > maxY) maxY = pos.y
    }

    // Tạo danh sách links
    for (const l of rawLinks) {
      const p1 = posMap[l.source]
      const p2 = posMap[l.target]
      if (p1 && p2) {
        positionedLinks.push({
          sourceId: l.source,
          targetId: l.target,
          x1: p1.x,
          y1: p1.y,
          x2: p2.x,
          y2: p2.y,
          midX: (p1.x + p2.x) / 2,
          midY: (p1.y + p2.y) / 2,
        })
      }
    }

    const padX = 65
    const padY = 60
    const vbWidth = Math.max(460, maxX - minX + padX * 2)
    const vbHeight = Math.max(280, maxY - minY + padY * 2)
    const vbX = (minX + maxX) / 2 - vbWidth / 2
    const vbY = (minY + maxY) / 2 - vbHeight / 2

    return {
      nodes: positionedNodes,
      links: positionedLinks,
      viewBox: `${vbX} ${vbY} ${vbWidth} ${vbHeight}`,
    }
  }, [character, compGraph, hanVietMap, hanViet, meaningVi, compDetails])

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      width={720}
      centered
      destroyOnClose
      className="kanji-breakdown-modal"
    >
      <div className="pt-1 pb-2 space-y-4">
        {/* Header Title */}
        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
          <div className="flex items-baseline gap-2.5">
            <h3 className="text-base font-bold text-gray-900 font-sans">
              Phân tích: <span className="font-serif text-xl font-black text-indigo-700">{character}</span>
            </h3>
          </div>
        </div>

        {/* Khung sơ đồ cây phân rã (SVG Graph) */}
        <div className="bg-slate-50/70 border border-gray-200/80 rounded-xl p-4 flex items-center justify-center min-h-[360px] overflow-hidden select-none">
          {nodes.length <= 1 && (!compGraph || !compGraph.links || compGraph.links.length === 0) ? (
            <div className="text-center py-12 space-y-3">
              <div className="w-16 h-16 rounded-full bg-indigo-50 border-2 border-indigo-500 flex items-center justify-center text-3xl font-serif font-bold text-gray-900 mx-auto shadow-xs">
                {character}
              </div>
              <p className="text-sm text-gray-500 font-medium">
                Chữ <span className="font-bold text-gray-700">{character}</span> là một bộ thủ độc lập cơ bản, không phân rã thêm thành phần con.
              </p>
            </div>
          ) : (
            <svg
              viewBox={viewBox}
              className="w-full h-[360px] max-w-full drop-shadow-2xs transition-all duration-200"
            >
              {/* 1. Vẽ các đường nối (Edges) */}
              {links.map((link, idx) => (
                <g key={`link-${idx}`}>
                  {/* Đường kẻ */}
                  <line
                    x1={link.x1}
                    y1={link.y1}
                    x2={link.x2}
                    y2={link.y2}
                    stroke="var(--color-slate-300)"
                    strokeWidth="2"
                    strokeLinecap="round"
                  />
                  {/* Chấm tròn nhỏ ở điểm giữa đường kẻ như ảnh Mazii */}
                  <circle
                    cx={link.midX}
                    cy={link.midY}
                    r="3"
                    fill="var(--color-slate-400)"
                  />
                </g>
              ))}

              {/* 2. Vẽ các vòng tròn Node */}
              {nodes.map((node) => {
                const nodeRadius = node.isRoot ? 26 : 24
                const tooltipTitle = (
                  <div className="max-w-xs sm:max-w-sm text-left p-1 text-xs leading-relaxed space-y-1">
                    <div className="font-bold text-sm text-amber-300 flex items-center gap-1.5 flex-wrap">
                      <span>[{node.word}]</span>
                      {node.hanViet && <span>[{node.hanViet}]</span>}
                    </div>
                    {node.detail ? (
                      <div className="text-gray-100 whitespace-pre-line text-[11px] leading-5 max-h-48 overflow-y-auto pr-1">
                        {node.detail}
                      </div>
                    ) : (
                      <div className="text-gray-300 text-[11px]">
                        {node.hanViet ? `Thành phần Hán tự: ${node.hanViet}` : 'Thành phần bộ thủ / nét chữ'}
                      </div>
                    )}
                  </div>
                )

                return (
                  <Tooltip
                    key={node.id}
                    title={tooltipTitle}
                    placement="top"
                    color="var(--color-sumi)"
                    mouseEnterDelay={0.08}
                    mouseLeaveDelay={0.15}
                    overlayInnerStyle={{
                      padding: '10px 14px',
                      borderRadius: '8px',
                      boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.4), 0 8px 10px -6px rgba(0, 0, 0, 0.3)',
                    }}
                  >
                    <g
                      className={!node.isRoot && onSelectCharacter ? 'cursor-pointer group' : 'group cursor-default'}
                      onClick={() => {
                        if (!node.isRoot && onSelectCharacter) {
                          onSelectCharacter(node.word)
                        }
                      }}
                    >
                      {/* Vòng tròn nền (giữ nguyên vị trí cố định, không dùng scale tránh rung lắc chuột trong SVG) */}
                      <circle
                        cx={node.x}
                        cy={node.y}
                        r={nodeRadius}
                        fill={node.isRoot ? 'var(--color-indigo-100)' : 'var(--color-surface)'}
                        stroke={node.isRoot ? 'var(--color-blue-500)' : 'var(--color-slate-700)'}
                        strokeWidth={node.isRoot ? 2.5 : 1.75}
                        className="transition-colors duration-150 group-hover:stroke-indigo-600 group-hover:fill-indigo-50/50"
                      />

                      {/* Chữ Hán bên trong */}
                      <text
                        x={node.x}
                        y={node.y + 7}
                        textAnchor="middle"
                        fill="var(--color-sumi)"
                        fontSize={node.isRoot ? '22' : '20'}
                        fontWeight="bold"
                        fontFamily="serif"
                        className="pointer-events-none select-none"
                      >
                        {node.word}
                      </text>
                    </g>
                  </Tooltip>
                )
              })}
            </svg>
          )}
        </div>

        {/* Chú giải thành phần Hán Việt ở chân modal */}
        {compDetails && compDetails.length > 0 && (
          <div className="pt-2 flex flex-wrap items-center gap-2 text-xs text-gray-600">
            <span className="font-bold text-gray-700">Các thành phần:</span>
            {compDetails.map((c, i) => {
              const compTooltip = (
                <div className="max-w-xs text-left p-1 text-xs leading-relaxed space-y-1">
                  <div className="font-bold text-sm text-amber-300 flex items-center gap-1.5">
                    <span>[{c.w}]</span>
                    {c.h && <span>[{c.h}]</span>}
                  </div>
                  {c.d ? (
                    <div className="text-gray-100 whitespace-pre-line text-[11px] leading-5 max-h-40 overflow-y-auto pr-1">
                      {c.d}
                    </div>
                  ) : (
                    <div className="text-gray-300 text-[11px]">
                      {c.h ? `Thành phần Hán tự: ${c.h}` : 'Bộ thủ / nét chữ'}
                    </div>
                  )}
                </div>
              )
              return (
                <Tooltip
                  key={i}
                  title={compTooltip}
                  placement="top"
                  color="var(--color-sumi)"
                  overlayInnerStyle={{ padding: '8px 12px', borderRadius: '8px' }}
                >
                  <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-gray-100 hover:bg-indigo-50 hover:border-indigo-300 text-gray-800 border border-gray-200 cursor-pointer transition">
                    <span className="font-serif font-bold text-sm">{c.w}</span>
                    {c.h && <span className="font-semibold text-gray-600">({c.h})</span>}
                  </span>
                </Tooltip>
              )
            })}
          </div>
        )}
      </div>
    </Modal>
  )
}
export default KanjiBreakdownModal

