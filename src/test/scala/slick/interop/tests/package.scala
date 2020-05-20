package slick.interop

import zio.Has

package object tests {
  type ItemRepository = Has[ItemRepository.Service]
}
