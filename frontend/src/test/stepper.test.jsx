import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import Stepper from '../components/Stepper'

function step(n, status) {
  return { stepNumber: n, status }
}

describe('Stepper', () => {
  it('renders all five step labels', () => {
    render(<Stepper steps={[]} />)
    ;['Style', 'Characters', 'Portraits', 'Chapters', 'Illustrations'].forEach(
      (label) => expect(screen.getByText(label)).toBeInTheDocument()
    )
  })

  it('marks a running step', () => {
    const steps = [
      step(1, 'DONE'),
      step(2, 'RUNNING'),
      step(3, 'PENDING'),
      step(4, 'PENDING'),
      step(5, 'PENDING'),
    ]
    render(<Stepper steps={steps} />)
    expect(screen.getByText('Running…')).toBeInTheDocument()
  })

  it('marks a failed step', () => {
    const steps = [
      step(1, 'DONE'),
      step(2, 'FAILED'),
      step(3, 'PENDING'),
      step(4, 'PENDING'),
      step(5, 'PENDING'),
    ]
    render(<Stepper steps={steps} />)
    expect(screen.getByText('Failed')).toBeInTheDocument()
  })
})
