const fallbackImages = {
  Cat: "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=900&q=85",
  Dog: "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=900&q=85",
};

export default function PetImage({ pet, ...imageProps }) {
  const fallback = fallbackImages[pet.species] || fallbackImages.Dog;

  function useFallback(event) {
    const image = event.currentTarget;
    if (image.src !== fallback) image.src = fallback;
    else image.onerror = null;
  }

  return (
    <img
      {...imageProps}
      src={pet.imageUrl || pet.image_url || fallback}
      alt={imageProps.alt ?? pet.name}
      onError={useFallback}
    />
  );
}
