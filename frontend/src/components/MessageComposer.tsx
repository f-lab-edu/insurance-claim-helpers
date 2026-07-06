import { useState, type KeyboardEvent } from 'react';
import styles from './MessageComposer.module.css';

interface MessageComposerProps {
  disabled: boolean;
  placeholder: string;
  onSend: (content: string) => void;
}

export default function MessageComposer({ disabled, placeholder, onSend }: MessageComposerProps) {
  const [text, setText] = useState('');

  function handleSend() {
    const trimmed = text.trim();
    if(disabled || trimmed.length === 0) {
      return;
    }
    onSend(trimmed);
    setText('');
  }

  // Enter = 전송, Shift+Enter = 줄바꿈.
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if(event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  }

  return (
    <div className={styles.composer}>
      <textarea
        className={styles.input}
        value={text}
        onChange={(event) => setText(event.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        rows={1}
      />
      <button
        type="button"
        className={styles.send}
        onClick={handleSend}
        disabled={disabled || text.trim().length === 0}
      >
        전송
      </button>
    </div>
  );
}