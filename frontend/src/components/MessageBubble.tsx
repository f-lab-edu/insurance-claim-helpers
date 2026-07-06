import type { MessageRole } from '../api/chat';
import styles from './MessageBubble.module.css';

interface MessageBubbleProps {
  role: MessageRole;
  content: string;
  streaming?: boolean;
}

export default function MessageBubble({ role, content, streaming = false }: MessageBubbleProps) {
  const isUser = role === 'USER';
  const rowClass = isUser ? styles.rowUser : styles.rowAssistant;
  const bubbleClass = isUser ? styles.bubbleUser : styles.bubbleAssistant;

  return (
    <div className={rowClass}>
      <div className={bubbleClass}>
        {content}
        {streaming && <span className={styles.cursor} aria-hidden="true">▍</span>}
      </div>
    </div>
  );
}