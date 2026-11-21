import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Card } from './Card';

describe('Card', () => {
  it('debe renderizar su contenido', () => {
    render(<Card>Contenido de la tarjeta</Card>);

    expect(screen.getByText('Contenido de la tarjeta')).toBeInTheDocument();
  });

  it('debe mostrar el título cuando se proporciona', () => {
    render(<Card title="Título de prueba">Contenido</Card>);

    expect(screen.getByRole('heading', { name: /Título de prueba/i })).toBeInTheDocument();
  });

  it('debe aplicar clases adicionales mediante className', () => {
    const { container } = render(<Card className="custom-card">Contenido</Card>);

    expect(container.firstChild).toHaveClass('custom-card');
  });

  it('no debe renderizar título cuando no se proporciona', () => {
    render(<Card>Sin título</Card>);

    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  });
});
