import android.content.Context
import com.connor.hindsightmobile.DB
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


fun observeNumFrames(context: Context): Flow<Int> = flow {
    val dbHelper = DB.getInstance(context)
    while (true) {
        dbHelper.getNumFrames()?.let { numFrames ->
            emit(numFrames)
        }
        delay(1000)
    }
}