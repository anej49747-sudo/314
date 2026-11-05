import { projectAttributes } from '../discovery/presentationRules.js'
import '../ui/discovery.css'

/** Equal visual treatment for every classification; status remains separate. */
export default function ProjectAttributes({ project, secondary = false }) {
  const fields = projectAttributes(project, { secondary })
  if (!fields.length) return null
  return <dl className="discovery-project-attributes" aria-label="Project classifications">
    {fields.map(field => <div className="discovery-project-attribute" key={field.key}>
      <dt>{field.label}</dt><dd>{field.value}</dd>
    </div>)}
  </dl>
}
