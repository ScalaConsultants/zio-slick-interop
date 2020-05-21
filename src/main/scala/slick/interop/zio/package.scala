package slick.interop

import _root_.zio.Has

package object zio {
  type DatabaseProvider = Has[DatabaseProvider.Service]
}
