// url=<MOMENTRA>?node-id=2-32
// source=momentra/momentra/Shell/BusinessCreate/BusinessGuidedCreateView.swift
// component=BusinessContinueButton
import figma from 'figma'

const instance = figma.selectedInstance

// Published component `Continue` / `Onboarding / Navigation`.
// TEXT prop drives the CTA label; solid emerald primary matches Obsidian guided create.
const label = instance.getString('Text')

export default {
  example: figma.swift`BusinessContinueButton(
    label: "${label}",
    enabled: true,
    action: {}
)`,
  imports: ['import SwiftUI'],
  id: 'business-continue-button',
  metadata: {
    nestable: true,
    props: {
      label,
    },
  },
}
