import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Input } from './Input';

describe('Input', () => {
  it('debe renderizar el campo de entrada', () => {
    render(<Input placeholder="Escribe aquí" />);

    expect(screen.getByPlaceholderText('Escribe aquí')).toBeInTheDocument();
  });

  it('debe mostrar la etiqueta cuando se proporciona', () => {
    render(<Input label="Correo electrónico" />);

    expect(screen.getByText('Correo electrónico')).toBeInTheDocument();
  });

  it('debe reflejar el valor y llamar onChange al escribir', () => {
    const handleChange = vi.fn();
    render(<Input value="inicial" onChange={handleChange} />);

    const input = screen.getByDisplayValue('inicial');
    fireEvent.change(input, { target: { value: 'nuevo valor' } });

    expect(handleChange).toHaveBeenCalledTimes(1);
  });

  it('debe respetar el estado deshabilitado', () => {
    render(<Input disabled />);

    expect(screen.getByRole('textbox')).toBeDisabled();
  });

  it('debe aplicar clases adicionales mediante className', () => {
    const { container } = render(<Input className="custom-input" />);

    expect(container.querySelector('input')).toHaveClass('custom-input');
  });
});
