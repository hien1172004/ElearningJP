import React from 'react'
import { Result, Button as AntdButton, Typography } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'

interface ErrorBoundaryProps {
  children: React.ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error?: Error
}

export class ErrorBoundary extends React.Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    // Có thể hook vào Sentry / logger ở đây sau này.
    console.error('[ErrorBoundary]', error, info)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined })
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
          <Result
            status="error"
            title="Đã xảy ra lỗi không mong muốn"
            subTitle="Ứng dụng gặp sự cố. Vui lòng thử lại hoặc tải lại trang."
            extra={[
              <AntdButton key="retry" onClick={this.handleReset}>
                Thử lại
              </AntdButton>,
              <AntdButton
                key="reload"
                type="primary"
                icon={<ReloadOutlined />}
                onClick={this.handleReload}
              >
                Tải lại trang
              </AntdButton>,
            ]}
          >
            {this.state.error?.message && (
              <Typography.Paragraph
                copyable={{ text: this.state.error.message }}
                style={{
                  background: '#fff1f2',
                  border: '1px solid #fecdd3',
                  color: '#9f1239',
                  padding: 12,
                  borderRadius: 8,
                  fontSize: 12,
                  maxHeight: 160,
                  overflow: 'auto',
                  textAlign: 'left',
                  margin: 0,
                }}
              >
                <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                  {this.state.error.message}
                </pre>
              </Typography.Paragraph>
            )}
          </Result>
        </div>
      )
    }

    return this.props.children
  }
}
