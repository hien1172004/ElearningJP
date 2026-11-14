export const PAYWALL_TRIGGER_EVENT = 'elearning:paywall:trigger'

export interface PaywallTriggerDetail {
  reason?: string
  feature?: string
}

export function emitPaywallTrigger(detail?: PaywallTriggerDetail): void {
  if (typeof window === 'undefined') return
  try {
    window.dispatchEvent(
      new CustomEvent<PaywallTriggerDetail>(PAYWALL_TRIGGER_EVENT, {
        detail: detail || { reason: 'Tính năng này yêu cầu nâng cấp gói PRO' },
      }),
    )
  } catch {
    /* ignore */
  }
}

