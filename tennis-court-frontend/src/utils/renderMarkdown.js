import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.use({
  gfm: true,
  breaks: true
})

/**
 * 将助手返回的 Markdown 转为可安全插入页面的 HTML（防 XSS）。
 */
export function renderAssistantMarkdown(text) {
  if (text == null || text === '') return ''
  const html = marked.parse(String(text))
  return DOMPurify.sanitize(html)
}
