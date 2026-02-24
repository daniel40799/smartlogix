import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux'
import type { RootState, AppDispatch } from './index'

/**
 * Type-safe replacement for the plain {@code useDispatch} hook.
 * Returns the store's {@link AppDispatch} type so that dispatching async thunks
 * is fully typed without extra casting.
 */
export const useAppDispatch = () => useDispatch<AppDispatch>()

/**
 * Type-safe replacement for the plain {@code useSelector} hook.
 * Pre-typed with {@link RootState} so selector callbacks receive full autocomplete
 * and type checking without needing to manually annotate the state parameter.
 */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector
