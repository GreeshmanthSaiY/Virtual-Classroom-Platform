import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from '../shared/components/Button';
import { Input } from '../shared/components/Input';
import { StatusBadge } from '../shared/components/Badge';
import { Pagination } from '../shared/components/Pagination';

describe('Shared Design System Components', () => {
  it('renders Button with variants and loading state', () => {
    const handleClick = vi.fn();
    const { rerender } = render(
      <Button variant="primary" onClick={handleClick}>
        Submit Work
      </Button>
    );

    const btn = screen.getByRole('button', { name: /submit work/i });
    expect(btn).toBeInTheDocument();
    fireEvent.click(btn);
    expect(handleClick).toHaveBeenCalledTimes(1);

    rerender(
      <Button variant="primary" isLoading onClick={handleClick}>
        Submit Work
      </Button>
    );
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('renders Input with label, error, and accessible ARIA attributes', () => {
    render(
      <Input
        label="Student Email"
        placeholder="student@university.edu"
        error="Invalid email format"
        id="email-input"
      />
    );

    const input = screen.getByPlaceholderText('student@university.edu');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Invalid email format');
  });

  it('renders Badge and StatusBadge correctly', () => {
    render(<StatusBadge status="ACTIVE" />);
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();

    render(<StatusBadge status="PENDING" />);
    expect(screen.getByText('PENDING')).toBeInTheDocument();

    render(<StatusBadge status="SUSPENDED" />);
    expect(screen.getByText('SUSPENDED')).toBeInTheDocument();
  });

  it('renders Pagination and triggers page navigation', () => {
    const handlePageChange = vi.fn();
    render(
      <Pagination
        currentPage={1}
        totalPages={5}
        totalItems={50}
        pageSize={10}
        onPageChange={handlePageChange}
      />
    );

    expect(screen.getByText(/Page 2 of 5/i)).toBeInTheDocument();
    const nextBtn = screen.getByRole('button', { name: /next page/i });
    fireEvent.click(nextBtn);
    expect(handlePageChange).toHaveBeenCalledWith(2);

    const prevBtn = screen.getByRole('button', { name: /previous page/i });
    fireEvent.click(prevBtn);
    expect(handlePageChange).toHaveBeenCalledWith(0);
  });
});
