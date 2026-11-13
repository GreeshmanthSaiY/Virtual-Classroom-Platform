import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { ThemeToggle } from '../shared/components/ThemeToggle';
import { ThemeProvider } from '../context/ThemeContext';

describe('ThemeToggle Design System Component', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.className = '';
  });

  it('renders theme toggle button with accessible aria-label', () => {
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    );

    const btn = screen.getByRole('button', { name: /switch to/i });
    expect(btn).toBeInTheDocument();
  });

  it('toggles theme state and updates document element class', () => {
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>
    );

    const btn = screen.getByRole('button', { name: /switch to/i });
    const initialTheme = document.documentElement.classList.contains('dark') ? 'dark' : 'light';

    fireEvent.click(btn);

    if (initialTheme === 'dark') {
      expect(document.documentElement.classList.contains('dark')).toBe(false);
      expect(localStorage.getItem('vc_theme')).toBe('light');
    } else {
      expect(document.documentElement.classList.contains('dark')).toBe(true);
      expect(localStorage.getItem('vc_theme')).toBe('dark');
    }
  });
});
