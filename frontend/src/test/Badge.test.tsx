import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { Badge } from '../components/common/Badge';

describe('Badge Component', () => {
  it('renders children correctly', () => {
    render(<Badge>Active Scholar</Badge>);
    expect(screen.getByText('Active Scholar')).toBeInTheDocument();
  });

  it('applies variant classes', () => {
    const { container } = render(<Badge variant="success">Approved</Badge>);
    expect(container.firstChild).toHaveClass('text-emerald-700');
  });

  it('applies danger variant classes', () => {
    const { container } = render(<Badge variant="danger">Rejected</Badge>);
    expect(container.firstChild).toHaveClass('text-rose-700');
  });
});

