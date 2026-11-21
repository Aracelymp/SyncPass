import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from './Button';

describe('Button', () => {
  it('debe renderizar el texto proporcionado', () => {
    render(<Button>Guardar</Button>);

    expect(screen.getByRole('button', { name: /Guardar/i })).toBeInTheDocument();
  });

  it('debe aplicar la clase según la variante', () => {
    const { rerender } = render(<Button variant="primary">Primario</Button>);
    expect(screen.getByRole('button')).toHaveClass('btn-primary');

    rerender(<Button variant="secondary">Secundario</Button>);
    expect(screen.getByRole('button')).toHaveClass('btn-secondary');

    rerender(<Button variant="outline">Contorno</Button>);
    expect(screen.getByRole('button')).toHaveClass('btn-outline');
  });

  it('debe aplicar clases adicionales mediante className', () => {
    render(<Button className="extra-class">Botón</Button>);

    expect(screen.getByRole('button')).toHaveClass('extra-class');
  });

  it('debe ejecutar onClick al hacer clic', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Haz clic</Button>);

    fireEvent.click(screen.getByRole('button'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('debe respetar el estado deshabilitado', () => {
    render(<Button disabled>Deshabilitado</Button>);

    expect(screen.getByRole('button')).toBeDisabled();
  });
});
