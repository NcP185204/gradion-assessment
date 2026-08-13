import { imageUrl } from '../api/client'

export default function ChapterCard({ chapter, projectId }) {
  const src = imageUrl(projectId, chapter.illustrationPath)
  return (
    <article className="card chapter-card">
      <div className="card-image">
        {src ? (
          <img src={src} alt={`Illustration for ${chapter.name}`} loading="lazy" />
        ) : (
          <div className="card-image-placeholder" aria-hidden="true">
            Illustration pending
          </div>
        )}
      </div>
      <div className="card-body">
        <h3 className="card-title">{chapter.name}</h3>
        <p className="card-prompt">{chapter.imagePrompt}</p>
      </div>
    </article>
  )
}