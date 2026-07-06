import { useEffect } from 'react';
import styles from './Toast.module.css';

interface ToastProps {
  message: string | null;
  onClose: () => void;
  durationMs?: number;
}

export default function Toast({ message, onClose, durationMs = 3000 }: ToastProps) {
  useEffect(() => {
    if(message === null) {
      return;
    }
    const timer = setTimeout(onClose, durationMs);
    return () => clearTimeout(timer);
  }, [message, durationMs, onClose]);

  if(message === null) {
    return null;
  }

  return (
    <div className={styles.toast} role="alert" onClick={onClose}>
      {message}
    </div>
  );
}