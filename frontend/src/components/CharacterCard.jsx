import { imageUrl } from '../api/client'

export default function CharacterCard({ character, projectId }) {
  const src = imageUrl(projectId, character.portraitPath)
  return (
    <article className="card character-card">
      <div className="card-image">
        {src ? (
          <img src={src} alt={`Portrait of ${character.name}`} loading="lazy" />
        ) : (
          <div className="card-image-placeholder" aria-hidden="true">
            Portrait pending
          </div>
        )}
      </div>
      <div className="card-body">
        <h3 className="card-title">{character.name}</h3>
        <p className="card-prompt">{character.imagePrompt}</p>
      </div>
    </article>
  )
}