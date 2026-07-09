import { useEffect, useRef, type SyntheticEvent } from 'react';
import styles from './ConfirmModal.module.css';

interface ConfirmModalProps {
  open: boolean;
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
}: Readonly<ConfirmModalProps>) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if(dialog === null) {
      return;
    }
    if(open && !dialog.open) {
      dialog.showModal();
    } else if(!open && dialog.open) {
      dialog.close();
    }
  }, [open]);
  
  function handleCancel(event: SyntheticEvent<HTMLDialogElement>) {
    event.preventDefault();
    onCancel();
  }

  return (
    <dialog ref={dialogRef} className={styles.dialog} onCancel={handleCancel}>
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <button className={styles.cancel} onClick={onCancel}>{cancelLabel}</button>
        <button className={styles.confirm} onClick={onConfirm}>{confirmLabel}</button>
      </div>
    </dialog>
  );
}