import { describe, it, expect, vi } from 'vitest';
import { runExchangeOnce, clearExchangeLock } from './authExchangeLock';

describe('authExchangeLock', () => {
  it('debe ejecutar la función y devolver su resultado', async () => {
    const fn = vi.fn().mockResolvedValue('token');
    const result = await runExchangeOnce(fn);

    expect(result).toBe('token');
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('debe reutilizar la promesa si se llama dos veces seguidas', async () => {
    const fn = vi.fn().mockResolvedValue('token');

    const promise1 = runExchangeOnce(fn);
    const promise2 = runExchangeOnce(fn);

    const [result1, result2] = await Promise.all([promise1, promise2]);

    expect(result1).toBe('token');
    expect(result2).toBe('token');
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it('debe liberar el lock al finalizar', async () => {
    const fn = vi.fn().mockResolvedValue('token');

    await runExchangeOnce(fn);
    const result = await runExchangeOnce(fn);

    expect(result).toBe('token');
    expect(fn).toHaveBeenCalledTimes(2);
  });

  it('debe liberar el lock incluso si la función falla', async () => {
    const fn = vi.fn().mockRejectedValue(new Error('fail'));

    await expect(runExchangeOnce(fn)).rejects.toThrow('fail');

    const fn2 = vi.fn().mockResolvedValue('token');
    const result = await runExchangeOnce(fn2);

    expect(result).toBe('token');
    expect(fn2).toHaveBeenCalledTimes(1);
  });

  it('debe permitir limpiar el lock manualmente', async () => {
    const fn = vi.fn().mockResolvedValue('token');

    runExchangeOnce(fn);
    clearExchangeLock();

    const result = await runExchangeOnce(fn);
    expect(result).toBe('token');
    expect(fn).toHaveBeenCalledTimes(2);
  });
});
