import { Link } from 'react-router-dom';
import { EmptyState } from '../components/common/PageState';

export function NotFoundPage() {
  return (
    <EmptyState
      title="Pagina non trovata"
      message="L'indirizzo non corrisponde a nessuna pagina della dashboard."
      action={<Link to="/">Torna alla casa</Link>}
    />
  );
}
