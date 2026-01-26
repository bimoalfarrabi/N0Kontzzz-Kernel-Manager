package id.jo.jomanager.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppProfileDao {
    @Query("SELECT * FROM app_profiles ORDER BY appName ASC")
    fun getAllProfiles(): Flow<List<AppProfileEntity>>

    @Query("SELECT * FROM app_profiles WHERE packageName = :packageName")
    suspend fun getProfile(packageName: String): AppProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AppProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: AppProfileEntity)
    
    @Query("DELETE FROM app_profiles WHERE packageName = :packageName")
    suspend fun deleteProfileByPackage(packageName: String)
}
