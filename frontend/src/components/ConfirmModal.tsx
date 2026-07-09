import styles from './ConfirmModal.module.css';

interface ConfirmModalProps {
  open: boolean;            // false 면 렌더하지 않음
  title: string;
  message: string;
  confirmLabel?: string;    // 기본 '삭제'
  cancelLabel?: string;     // 기본 '취소'
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmModal({
  open,
  title,
  message,
  confirmLabel = '삭제',
  cancelLabel = '취소',
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  if(!open) {
    return null;
  }

  return (
    // 배경(오버레이) 클릭 시 취소
    <div className={styles.overlay} onClick={onCancel}>
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 className={styles.title}>{title}</h2>
        <p className={styles.message}>{message}</p>
        <div className={styles.actions}>
          <button className={styles.cancel} onClick={onCancel}>{cancelLabel}</button>
          <button className={styles.confirm} onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}