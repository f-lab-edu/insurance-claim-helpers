import styles from './ClaimCriteriaChip.module.css';

interface ClaimCriteriaChipProps {
  fileName: string;
  onRemove: () => void;
  removing?: boolean;
}

export default function ClaimCriteriaChip({ fileName, onRemove, removing = false }: ClaimCriteriaChipProps) {
  return (
    <span className={styles.chip}>
      {/* 📎 문서 아이콘 */}
      <svg
        className={styles.icon}
        width="14" height="14" viewBox="0 0 24 24"
        fill="none" stroke="currentColor" strokeWidth="1.8"
        strokeLinecap="round" strokeLinejoin="round"
      >
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <polyline points="14 2 14 8 20 8" />
      </svg>

      <span className={styles.name}>{fileName}</span>

      <button
        type="button"
        className={styles.remove}
        onClick={onRemove}
        disabled={removing}
        aria-label={`${fileName} 연결 해제`}
      >
        ✕
      </button>
    </span>
  );
}